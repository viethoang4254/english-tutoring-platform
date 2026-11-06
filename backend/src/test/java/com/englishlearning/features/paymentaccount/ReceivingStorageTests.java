package com.englishlearning.features.paymentaccount;

import com.englishlearning.features.paymentaccount.storage.SupabaseQrStorage;
import com.englishlearning.features.paymentaccount.service.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceivingStorageTests {
    static final String OBJECT="12345678-1234-1234-1234-123456789abc.png";
    static final byte[] PRIVATE="{\"id\":\"bank-qr-private\",\"public\":false}".getBytes(StandardCharsets.UTF_8);
    record Reply(int status,byte[] body) {}
    static class Fake extends SupabaseQrStorage {
        Queue<Reply> replies=new ArrayDeque<>();
        List<HttpRequest> requests=new ArrayList<>();
        CompletableFuture<HttpResponse<byte[]>> stalled;
        Flow.Subscription lastSubscription;
        Fake(String origin){super(origin,"bank-qr-private","test-only",tools.jackson.databind.json.JsonMapper.builder().build());}
        @Override protected Duration operationTimeout(){return Duration.ofSeconds(15);}
        @Override protected CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request,HttpResponse.BodyHandler<byte[]> handler){
            requests.add(request);
            if(replies.isEmpty())return stalled;
            var reply=replies.remove();
            var subscriber=handler.apply(mock(HttpResponse.ResponseInfo.class));
            lastSubscription=mock(Flow.Subscription.class);
            subscriber.onSubscribe(lastSubscription);
            subscriber.onNext(List.of(ByteBuffer.wrap(reply.body())));
            subscriber.onComplete();
            return subscriber.getBody().toCompletableFuture().thenApply(bytes->{
                @SuppressWarnings("unchecked") HttpResponse<byte[]> response=mock(HttpResponse.class);
                when(response.statusCode()).thenReturn(reply.status());when(response.body()).thenReturn(bytes);return response;
            });
        }
    }
    @Test void publicBucketAndRedirectFailClosed() {
        for(int status:new int[]{200,302}) {
            var s=new Fake("https://fixture.supabase.co");
            s.replies.add(new Reply(status,"{\"id\":\"bank-qr-private\",\"public\":true}".getBytes(StandardCharsets.UTF_8)));
            assertThrows(ReceivingFailure.class,()->s.put(OBJECT,ReceivingImageTests.png()));assertEquals(1,s.requests.size());
        }
    }
    @Test void privateUploadNeverUpsertsAndDownloadIsAuthenticated() {
        var s=new Fake("https://fixture.supabase.co");
        s.replies.add(new Reply(200,PRIVATE));s.replies.add(new Reply(200,"{}".getBytes(StandardCharsets.UTF_8)));s.replies.add(new Reply(200,PRIVATE));
        s.put(OBJECT,ReceivingImageTests.png());
        assertEquals("POST",s.requests.get(1).method());assertEquals("false",s.requests.get(1).headers().firstValue("x-upsert").orElseThrow());
        assertTrue(s.requests.get(1).timeout().orElseThrow().compareTo(Duration.ofSeconds(15))<=0);
        s.replies.add(new Reply(200,PRIVATE));s.replies.add(new Reply(200,ReceivingImageTests.png()));
        assertArrayEquals(ReceivingImageTests.png(),s.read(OBJECT));
        assertTrue(s.requests.get(4).uri().getPath().contains("/object/authenticated/"));
        assertTrue(s.requests.stream().noneMatch(r->r.uri().getPath().contains("/public/")));
    }
    @Test void invalidConfigurationPathsAndOversizedResponsesAreSanitized() {
        for(String origin:new String[]{"http://fixture.supabase.co","https://evil.invalid","https://fixture.supabase.co/"}) {
            var s=new Fake(origin);assertThrows(ReceivingFailure.class,()->s.read(OBJECT));assertTrue(s.requests.isEmpty());
        }
        var s=new Fake("https://fixture.supabase.co");assertThrows(ReceivingFailure.class,()->s.read("../secret"));assertTrue(s.requests.isEmpty());
        s.replies.add(new Reply(200,PRIVATE));s.replies.add(new Reply(200,new byte[QrImage.MAX+1]));
        var error=assertThrows(ReceivingFailure.class,()->s.read(OBJECT));assertEquals("Receiving account request failed.",error.getMessage());
        verify(s.lastSubscription).cancel();
    }
    @Test void stalledUploadTimesOutAndCancelsExchange() {
        var s=new Fake("https://fixture.supabase.co") {
            @Override protected Duration operationTimeout(){return Duration.ofSeconds(2);}
        };s.replies.add(new Reply(200,PRIVATE));s.stalled=new CompletableFuture<>();
        assertTimeout(Duration.ofSeconds(5),()->assertThrows(ReceivingFailure.class,()->s.put(OBJECT,ReceivingImageTests.png())));
        assertEquals("POST",s.requests.get(1).method());assertTrue(s.stalled.isCancelled());assertEquals(2,s.requests.size());
    }
    @Test void interruptedRequestCancelsAndRestoresInterruptFlag() {
        var s=new Fake("https://fixture.supabase.co");s.replies.add(new Reply(200,PRIVATE));
        s.stalled=new CompletableFuture<>() {
            @Override public HttpResponse<byte[]> get(long timeout,TimeUnit unit)throws InterruptedException {throw new InterruptedException();}
        };
        try {
            assertThrows(ReceivingFailure.class,()->s.put(OBJECT,ReceivingImageTests.png()));
            assertTrue(Thread.currentThread().isInterrupted());assertTrue(s.stalled.isCancelled());
        } finally {Thread.interrupted();}
    }
    @Test void cleanupDeletesOnlyExactObjectAndDoesNotRetryFailure() {
        var s=new Fake("https://fixture.supabase.co");s.replies.add(new Reply(200,PRIVATE));s.replies.add(new Reply(200,"{}".getBytes(StandardCharsets.UTF_8)));
        s.delete(OBJECT);assertEquals("DELETE",s.requests.get(1).method());
        assertEquals("/storage/v1/object/bank-qr-private/"+OBJECT,s.requests.get(1).uri().getPath());
        s.replies.add(new Reply(200,PRIVATE));s.replies.add(new Reply(503,new byte[0]));
        assertThrows(ReceivingFailure.class,()->s.delete(OBJECT));assertEquals(4,s.requests.size());
    }
}

package com.englishlearning.features.paymentaccount.storage;
import com.englishlearning.features.paymentaccount.service.*;
import java.net.*;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
@Component
public class SupabaseQrStorage implements PrivateQrStorage {
    private final String origin,bucket,key; private final JsonMapper json;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    public SupabaseQrStorage(@Value("${app.bank-qr.storage-origin:}") String origin,
        @Value("${app.bank-qr.bucket:}") String bucket,@Value("${app.bank-qr.service-key:}") String key,JsonMapper json) {
        this.origin=origin;this.bucket=bucket;this.key=key;this.json=json;
    }
    private void configured() {
        try {
            URI uri=URI.create(origin);
            if(!"https".equals(uri.getScheme()) || uri.getHost()==null || !uri.getHost().matches("[a-z0-9-]+\\.supabase\\.co")
                || uri.getPort()!=-1 || uri.getRawUserInfo()!=null || uri.getRawQuery()!=null || uri.getRawFragment()!=null
                || !uri.getPath().isEmpty() || !bucket.matches("[a-z0-9][a-z0-9-]{2,62}") || key.isBlank()
                || key.contains("\r") || key.contains("\n")) throw new IllegalArgumentException();
        } catch(Exception e) { throw unavailable(); }
    }
    private void privateBucket(long deadline) {
        configured();
        try {
            var row=json.readTree(exchange("GET","/bucket/"+bucket,null,65536,deadline));
            if(!row.path("id").asText().equals(bucket) || !row.path("public").isBoolean() || row.path("public").asBoolean()) throw unavailable();
        } catch(Exception e) { throw unavailable(); }
    }
    private void object(String object) {
        if(object==null || !object.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png")) throw unavailable();
    }
    @Override public void put(String object,byte[] png) {
        object(object);long deadline=deadline();privateBucket(deadline);
        if(png.length==0 || png.length>QrImage.MAX)throw unavailable();
        exchange("POST","/object/"+bucket+"/"+object,png,65536,deadline);
        privateBucket(deadline);
    }
    @Override public byte[] read(String object) {
        object(object);long deadline=deadline();privateBucket(deadline);
        return exchange("GET","/object/authenticated/"+bucket+"/"+object,null,QrImage.MAX,deadline);
    }
    @Override public void delete(String object) {
        object(object);
        long deadline = deadline();
        privateBucket(deadline);
        exchange("DELETE","/object/"+bucket+"/"+object,null,65536,deadline);
    }

    protected Duration operationTimeout() { return Duration.ofSeconds(15); }
    private long deadline() { return System.nanoTime()+operationTimeout().toNanos(); }
    protected CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request, HttpResponse.BodyHandler<byte[]> handler) {
        return client.sendAsync(request,handler);
    }

    // A single deadline covers bucket checks, request-body upload and the complete bounded response.
    private byte[] exchange(String method,String path,byte[] body,int limit,long deadline) {
        CompletableFuture<HttpResponse<byte[]>> pending = null;
        try {
            long remaining = deadline-System.nanoTime();
            if (remaining<=0) throw unavailable();
            var builder=HttpRequest.newBuilder(URI.create(origin+"/storage/v1"+path))
                .timeout(Duration.ofNanos(remaining)).header("apikey",key).header("Authorization","Bearer "+key);
            if(body!=null) builder.header("Content-Type","image/png").header("x-upsert","false");
            var request=builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body)).build();
            pending=send(request,info -> bounded(limit));
            remaining=deadline-System.nanoTime();
            if(remaining<=0) throw new TimeoutException();
            var response=pending.get(remaining,TimeUnit.NANOSECONDS);
            if(response.statusCode()<200||response.statusCode()>=300||response.body().length>limit) throw unavailable();
            return response.body();
        } catch(InterruptedException e) {
            if(pending!=null) pending.cancel(true);
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch(Exception e) {
            if(pending!=null) pending.cancel(true);
            throw unavailable();
        }
    }

    private static HttpResponse.BodySubscriber<byte[]> bounded(int limit) {
        return new HttpResponse.BodySubscriber<>() {
            final HttpResponse.BodySubscriber<byte[]> delegate=HttpResponse.BodySubscribers.ofByteArray();
            Flow.Subscription subscription;
            long size;
            boolean failed;
            public CompletionStage<byte[]> getBody(){return delegate.getBody();}
            public void onSubscribe(Flow.Subscription value){subscription=value;delegate.onSubscribe(value);}
            public void onNext(java.util.List<ByteBuffer> buffers){
                if(failed)return;
                for(var buffer:buffers)size+=buffer.remaining();
                if(size>limit){failed=true;subscription.cancel();delegate.onError(unavailable());}
                else delegate.onNext(buffers);
            }
            public void onError(Throwable failure){if(!failed){failed=true;delegate.onError(failure);}}
            public void onComplete(){if(!failed)delegate.onComplete();}
        };
    }
    private static ReceivingFailure unavailable(){return new ReceivingFailure(ReceivingFailure.Reason.UNAVAILABLE);}
}

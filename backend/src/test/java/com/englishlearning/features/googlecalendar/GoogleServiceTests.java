package com.englishlearning.features.googlecalendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.teacher.service.*;

class GoogleServiceTests {
    @TempDir Path folder;
    GoogleStore store;GoogleProvider provider;CurrentAccountService accounts;TeacherBusinessEligibility teachers;
    GoogleCrypto crypto;GoogleService service;String state,binding;
    @BeforeEach void setup()throws Exception{
        var settings=GoogleTestSettings.create(folder);crypto=new GoogleCrypto(settings);store=mock(GoogleStore.class);provider=mock(GoogleProvider.class);
        accounts=mock(CurrentAccountService.class);teachers=mock(TeacherBusinessEligibility.class);
        when(store.transaction(any())).thenAnswer(call->((Supplier<?>)call.getArgument(0)).get());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.STUDENT,false,false,true,false)));
        service=new GoogleService(settings,crypto,provider,store,accounts,teachers);state=crypto.secret();binding=crypto.secret();
    }
    void intent(){
        when(store.consume(GoogleCrypto.hash(state),GoogleCrypto.hash(binding))).thenReturn(new GoogleStore.Intent(GoogleCrypto.hash(state),1,
            crypto.encrypt("verifier","intent:"+GoogleCrypto.hash(state)),"test-1",GoogleCrypto.hash("nonce"),2));
    }
    GoogleStore.Connection connection(String subject,String status,long version){return new GoogleStore.Connection(subject,"synthetic@example.test","openid email "+GoogleSettings.SCOPE,
        crypto.encrypt("original-refresh","refresh:1:"+subject),"test-1",status,version,java.time.Instant.now());}
    @Test void studentAndApprovedUnverifiedTeacherCanStartButOtherRolesCannot(){
        when(store.advance(1)).thenReturn(2L);when(provider.authorize(anyString(),anyString(),anyString())).thenReturn("https://accounts.google.com/o/oauth2/v2/auth");
        assertNotNull(service.start(1));verify(store).lockEligible(1);
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,false,false,true)));
        assertNotNull(service.start(1));verify(teachers,atLeastOnce()).requireEligible(1L);
        for(var account:List.of(new AccountSnapshot(1L,Role.ADMIN,false,true,false,false),new AccountSnapshot(1L,Role.STUDENT,true,true,true,false),new AccountSnapshot(1L,Role.TEACHER,false,false,false,false))){
            when(accounts.findById(1L)).thenReturn(Optional.of(account));assertThrows(GoogleFailure.class,()->service.start(1));
        }
    }
    @Test void verifiedButUnapprovedTeacherStillRequiresApproval(){
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true,false,false)));
        doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teachers).requireEligible(1L);
        assertThrows(GoogleFailure.class,()->service.start(1));verifyNoInteractions(store,provider);
    }
    @Test void callbackPreservesSameSubjectRefreshWhenProviderOmitsIt(){
        intent();when(store.connection(1)).thenReturn(connection("subject","CONNECTED",2));
        when(provider.exchange(anyString(),anyString(),anyString())).thenReturn(new GoogleProvider.Grant("subject","synthetic@example.test","openid email "+GoogleSettings.SCOPE,null));
        when(store.save(eq(1L),eq(2L),any(),anyString(),eq("test-1"))).thenAnswer(call->{
            assertEquals("original-refresh",crypto.decrypt(call.getArgument(3),"test-1","refresh:1:subject"));return true;});
        service.complete(state,binding,"code",null);verify(store).save(eq(1L),eq(2L),any(),anyString(),eq("test-1"));
    }
    @Test void changingGoogleAccountCannotBorrowPreviousRefreshToken(){
        intent();when(store.connection(1)).thenReturn(connection("old","CONNECTED",2));
        when(provider.exchange(anyString(),anyString(),anyString())).thenReturn(new GoogleProvider.Grant("new","synthetic@example.test","scope",null));
        assertEquals("RECONNECT_REQUIRED",assertThrows(GoogleFailure.class,()->service.complete(state,binding,"code",null)).code);
        verify(store,never()).save(anyLong(),anyLong(),any(),anyString(),anyString());
    }
    @Test void newerIntentOrDisconnectInvalidatesInFlightCallback(){
        intent();when(store.connection(1)).thenReturn(connection("subject","DISCONNECTED",3));
        when(provider.exchange(anyString(),anyString(),anyString())).thenReturn(new GoogleProvider.Grant("subject","synthetic@example.test","scope","new-refresh"));
        assertEquals("EXPIRED_OR_INVALID",assertThrows(GoogleFailure.class,()->service.complete(state,binding,"code",null)).code);
        verify(store,never()).save(anyLong(),anyLong(),any(),anyString(),anyString());
    }
    @Test void denialConsumesIntentWithoutProviderExchange(){
        intent();assertEquals("CONSENT_DENIED",assertThrows(GoogleFailure.class,()->service.complete(state,binding,null,"access_denied")).code);
        verify(store).consume(GoogleCrypto.hash(state),GoogleCrypto.hash(binding));verifyNoInteractions(provider);
    }
    @Test void invalidBrowserStateCannotReachProvider(){
        assertThrows(GoogleFailure.class,()->service.complete(state,null,"code",null));verifyNoInteractions(store,provider);
    }
    @Test void localDisconnectRemainsDisabledWhenRemoteRevocationFails(){
        when(store.connection(1)).thenReturn(connection("subject","CONNECTED",2),connection("subject","DISCONNECTED",3));
        when(provider.revoke(anyString())).thenReturn(false);assertFalse(service.disconnect(1));
        var order=inOrder(store,provider);order.verify(store).disconnect(1);order.verify(provider).revoke("original-refresh");
    }
    @Test void invalidGrantMarksReconnectionAndCannotReturnAccess(){
        when(store.connection(1)).thenReturn(connection("subject","CONNECTED",2));when(provider.refresh(anyString())).thenThrow(new GoogleFailure("RECONNECT_REQUIRED"));
        assertThrows(GoogleFailure.class,()->service.accessToken(1));verify(store).reconnectRequired(1,2);
    }
    @Test void statusIsOwnerScopedAndContainsNoCredentials(){
        when(store.connection(1)).thenReturn(connection("subject","CONNECTED",2));var result=service.status(1);
        assertEquals("CONNECTED",result.status());assertFalse(result.synchronizationAvailable());assertFalse(result.toString().contains("refresh"));verify(store).connection(1);
        assertThrows(GoogleFailure.class,()->service.status(2));verify(store,never()).connection(2);
    }
}

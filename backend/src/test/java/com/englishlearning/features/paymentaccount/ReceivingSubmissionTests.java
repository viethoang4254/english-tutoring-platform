package com.englishlearning.features.paymentaccount;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.paymentaccount.dto.ReceivingAccount;
import com.englishlearning.features.paymentaccount.repository.ReceivingAccountRepository;
import com.englishlearning.features.paymentaccount.service.*;
import com.englishlearning.features.paymentaccount.storage.PrivateQrStorage;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceivingSubmissionTests {
    final ReceivingAccountRepository rows=mock(ReceivingAccountRepository.class);
    final TeacherBusinessEligibility eligibility=mock(TeacherBusinessEligibility.class);
    final UserCredentialService users=mock(UserCredentialService.class);
    final PrivateQrStorage storage=mock(PrivateQrStorage.class);
    final TestTransactions manager=new TestTransactions();
    final Instant now=Instant.parse("2026-11-05T00:00:00Z");
    ReceivingAccountService service;
    String uploaded;
    boolean lateDuplicate,lateReuse;

    static class TestTransactions extends AbstractPlatformTransactionManager {
        boolean uncertainCommit,failedRollback;
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object transaction,TransactionDefinition definition){}
        protected void doCommit(DefaultTransactionStatus status){if(uncertainCommit)throw new TransactionSystemException("test commit outcome unknown");}
        protected void doRollback(DefaultTransactionStatus status){if(failedRollback)throw new TransactionSystemException("test rollback outcome unknown");}
    }
    ReceivingAccountRepository.Stored row() {
        return new ReceivingAccountRepository.Stored(1,new ReceivingAccount(new ApiId(9),"970436","VCB","Bank","Teacher","0012345678","PENDING","INACTIVE",true,now),"historical.png","hash","hash");
    }
    @BeforeEach void setup() {
        var tx=new ReceivingAccountTransactions(rows,eligibility,users,Clock.fixed(now,ZoneOffset.UTC)) {
            @Override public ReceivingAccountService.Created persist(Submission input,Upload upload) {
                if(lateReuse) {
                    var r=row();when(rows.byKey(input.actor(),input.key())).thenReturn(Optional.of(new ReceivingAccountRepository.Stored(r.teacher(),r.account(),r.object(),r.imageHash(),input.fingerprint())));
                }
                return new TransactionTemplate(manager).execute(status->super.persist(input,upload));
            }
        };
        service=new ReceivingAccountService(rows,eligibility,mock(CurrentAccountService.class),
            new BankDirectory(tools.jackson.databind.json.JsonMapper.builder().build()),storage,tx);
        when(users.lockAccountById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true,true)));
        when(rows.insert(anyLong(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyInt(),any(),anyString(),any())).thenReturn(9L);
        when(rows.get(9)).thenReturn(Optional.of(row()));
        doAnswer(call->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            uploaded=call.getArgument(0);
            if(lateDuplicate)when(rows.pending(1)).thenReturn(true);
            return null;
        }).when(storage).put(anyString(),any());
        doAnswer(call->{assertFalse(TransactionSynchronizationManager.isActualTransactionActive());assertEquals(uploaded,call.getArgument(0));return null;}).when(storage).delete(anyString());
    }
    ReceivingAccountService.Created submit(){return service.submit(1,"970436","Teacher","0012345678",UUID.randomUUID().toString(),ReceivingImageTests.png());}
    void failInsert(RuntimeException failure) {
        when(rows.insert(anyLong(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyInt(),any(),anyString(),any())).thenThrow(failure);
    }
    @Test void successfulCommitRetainsEvidence(){assertTrue(submit().created());verify(storage,never()).delete(anyString());}
    @Test void insertFailureAfterConfirmedRollbackDeletesOnlyFreshUpload(){
        var failure=new IllegalStateException("test insert failure");failInsert(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,this::submit));verify(storage).delete(uploaded);
    }
    @Test void revalidationFailureRollsBackAndReclaimsUnusedUpload(){
        lateDuplicate=true;assertEquals(ReceivingFailure.Reason.CONFLICT,assertThrows(ReceivingFailure.class,this::submit).reason());
        verify(storage).delete(uploaded);
        verify(rows,never()).insert(anyLong(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyInt(),any(),anyString(),any());
    }
    @Test void cleanupFailureDoesNotHideInsertFailureOrRetry(){
        var failure=new IllegalStateException("test insert failure");failInsert(failure);
        doThrow(new ReceivingFailure(ReceivingFailure.Reason.UNAVAILABLE)).when(storage).delete(anyString());
        assertSame(failure,assertThrows(IllegalStateException.class,this::submit));verify(storage,times(1)).delete(uploaded);
    }
    @Test void concurrentIdempotentWinnerKeepsItsEvidenceAndUnusedUploadIsDeleted(){
        lateReuse=true;assertFalse(submit().created());verify(storage).delete(uploaded);
        assertNotEquals("historical.png",uploaded);
        verify(rows,never()).insert(anyLong(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyInt(),any(),anyString(),any());
    }
    @Test void uncertainCommitRetainsObject(){
        manager.uncertainCommit=true;assertThrows(TransactionSystemException.class,this::submit);verify(storage,never()).delete(anyString());
    }
    @Test void uncertainRollbackRetainsObject(){
        manager.failedRollback=true;failInsert(new IllegalStateException("test insert failure"));
        assertThrows(TransactionSystemException.class,this::submit);verify(storage,never()).delete(anyString());
    }
    @Test void uncertainUploadNeverStartsPersistenceOrDeletes(){
        doThrow(new ReceivingFailure(ReceivingFailure.Reason.UNAVAILABLE)).when(storage).put(anyString(),any());
        assertThrows(ReceivingFailure.class,this::submit);verifyNoInteractions(users);verify(storage,never()).delete(anyString());
    }
}

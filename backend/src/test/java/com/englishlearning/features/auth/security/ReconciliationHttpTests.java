package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.payment.dto.*;
import com.englishlearning.features.payment.repository.PaymentRepository;
import com.englishlearning.features.enrollment.dto.EnrollmentDto;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.context.SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "spring.flyway.enabled=false","app.security.browser.local-http=false","app.security.browser.allowed-origins=https://frontend.example"})
class ReconciliationHttpTests extends AuthenticationHttpTestSupport {
    static final String PATH="/api/v1/teacher/courses/2/payments/7";
    static final String REFERENCE="ELP0123456789ABCDEF012345";
    String origin(){return "https://frontend.example";}String api(){return "https://backend.example";}boolean secure(){return true;}
    void actor(Role role,boolean verified,boolean locked) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        var a=new AccountSnapshot(1L,role,locked,verified,true, role == Role.TEACHER && verified);
        when(accounts.findById(1L)).thenReturn(Optional.of(a));when(credentials.lockAccountById(1L)).thenReturn(Optional.of(a));
    }
    PaymentDto payment(){return new PaymentDto(new ApiId(7),new ApiId(3),"PENDING","VND","101","0","0","0","101",
        REFERENCE,new ApiId(8),"970436","Bank","00123456","Teacher","unused",now.plusSeconds(900),now);}
    String body(boolean attested,String amount){return """
        {"bankBin":"970436","accountNumber":"00123456","bankTransactionReference":"BANK-1",
         "receivedAt":"%s","observedAmount":%s,"currency":"VND","transferDescription":"%s",
         "transferReference":"%s","attested":%s}
        """.formatted(now,amount,REFERENCE,REFERENCE,attested);}
    void setup() {
        actor(Role.TEACHER,true,false);
        when(credentials.lockAccountById(9L)).thenReturn(Optional.of(new AccountSnapshot(9L,Role.STUDENT,false,false,true)));
        when(paymentRepository.teacherContext(1,2,7)).thenReturn(Optional.of(new PaymentRepository.ReconciliationContext(9,3)));
        var course=new EnrollmentRepository.Course(1,"PUBLISHED",new BigDecimal("101"),1);
        when(enrollmentRepository.course(2,true)).thenReturn(Optional.of(course));when(enrollmentRepository.course(2,false)).thenReturn(Optional.of(course));
        when(enrollmentRepository.firstStart(2)).thenReturn(now.plusSeconds(3600));when(enrollmentRepository.occupied(2,now)).thenReturn(1L);
        when(enrollmentRepository.forCourse(9,2,now)).thenReturn(Optional.of(new EnrollmentDto(new ApiId(3),new ApiId(2),"Course","PENDING",null,now.plusSeconds(900),false,null,null,now,now)));
        when(paymentRepository.lockPayment(9,7)).thenReturn(payment());when(paymentRepository.own(9,7)).thenReturn(Optional.of(payment()));
        when(paymentRepository.recordAttempt(eq(7L),eq(1L),any(),anyBoolean(),eq(now),nullable(String.class),nullable(Long.class))).thenAnswer(call->{
            String failure=call.getArgument(5);Long replay=call.getArgument(6);
            return new ReconciliationAttempt(new ApiId(10),new ApiId(7),new ApiId(1),call.getArgument(2),call.getArgument(3),now,
                failure==null?"SUCCESS":"FAILED",failure,failure==null&&replay==null,replay==null?null:new ApiId(replay));
        });
    }
    @Test void teacherConfirmationUsesAuthenticatedIdentityAndNoStore()throws Exception {
        setup();mvc.perform(post(PATH+"/confirm-manually").header("Authorization","Bearer access").contentType("application/json").content(body(true,"101")))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.outcome").value("SUCCESS")).andExpect(jsonPath("$.teacherId").value("1"));
        verify(paymentRepository).activate(7,3,now);
    }
    @Test void studentAdminAnonymousAndUnverifiedTeacherDenied()throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.STUDENT,Role.ADMIN)) {
            actor(role,true,false);mvc.perform(post(PATH+"/confirm-manually").header("Authorization","Bearer access").contentType("application/json").content(body(true,"101"))).andExpect(status().isForbidden());
        }
        actor(Role.TEACHER,false,false);mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        actor(Role.TEACHER,true,true);mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isUnauthorized());verifyNoInteractions(paymentRepository);
    }
    @Test void wrongTeacherCannotSeeOrReconcilePayment()throws Exception {
        actor(Role.TEACHER,true,false);
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isNotFound());
        mvc.perform(post(PATH+"/reconcile-late").header("Authorization","Bearer access").contentType("application/json").content(body(true,"101"))).andExpect(status().isNotFound());
        verify(paymentRepository,never()).activate(anyLong(),anyLong(),any());
    }
    @Test void businessFailureIsAuditedAndReturnedAsConflict()throws Exception {
        setup();mvc.perform(post(PATH+"/confirm-manually").header("Authorization","Bearer access").contentType("application/json").content(body(true,"100")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.failureReason").value("AMOUNT_MISMATCH"));
        verify(paymentRepository).recordAttempt(eq(7L),eq(1L),any(),eq(false),eq(now),eq("AMOUNT_MISMATCH"),isNull());
        verify(paymentRepository,never()).activate(anyLong(),anyLong(),any());
    }
    @Test void foreignReceiverCannotProbeAnAllocatedOrUnusedReference()throws Exception {
        setup();
        var allocated=new ReconciliationAttempt(new ApiId(88),new ApiId(99),new ApiId(5),
            null,false,now,"SUCCESS",null,true,null);
        when(paymentRepository.allocation(any(),eq("ALLOCATED"))).thenReturn(Optional.of(allocated));
        for(String candidate:List.of("ALLOCATED","UNUSED")) {
            String probe=body(true,"101").replace("00123456","00999999").replace("BANK-1",candidate);
            mvc.perform(post(PATH+"/confirm-manually").header("Authorization","Bearer access").contentType("application/json").content(probe))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.failureReason").value("RECEIVER_MISMATCH"));
        }
        verify(paymentRepository,times(2)).recordAttempt(eq(7L),eq(1L),any(),eq(false),eq(now),eq("RECEIVER_MISMATCH"),isNull());
        verify(paymentRepository,never()).lockBankTransaction(any(),anyString());
        verify(paymentRepository,never()).allocation(any(),anyString());
        verify(paymentRepository,never()).activate(anyLong(),anyLong(),any());
    }
    @Test void absentAndFalseAttestationCannotActivate()throws Exception {
        setup();mvc.perform(post(PATH+"/confirm-manually").header("Authorization","Bearer access").contentType("application/json").content(body(false,"101")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.failureReason").value("ATTESTATION_REQUIRED"));
        mvc.perform(post(PATH+"/confirm-manually").header("Authorization","Bearer access").contentType("application/json").content(body(true,"101").replace("\"attested\":true","\"attested\":null")))
            .andExpect(status().isBadRequest());verify(paymentRepository,never()).activate(anyLong(),anyLong(),any());
        verify(paymentRepository,never()).lockBankTransaction(any(),anyString());
        verify(paymentRepository,never()).allocation(any(),anyString());
    }
    @Test void queueDetailHistoryPaginationAndPrivateFields()throws Exception {
        setup();when(paymentRepository.teacherPage(1,2,0,20)).thenReturn(List.of(payment()));
        mvc.perform(get("/api/v1/teacher/courses/2/payments").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value("7")).andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.accountNumber").value("00123456")).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get(PATH+"/attempts").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get(PATH+"/attempts?size=101").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
    }
    @Test void infrastructureErrorsDoNotLeakEvidence()throws Exception {
        setup();when(paymentRepository.lockPayment(9,7)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("secret bank evidence"));
        mvc.perform(post(PATH+"/confirm-manually").header("Authorization","Bearer access").contentType("application/json").content(body(true,"101")))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PAYMENT_UNAVAILABLE"));
    }
}

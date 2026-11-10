package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.payment.dto.PaymentDto;
import com.englishlearning.features.payment.repository.PaymentRepository;
import com.englishlearning.features.enrollment.dto.EnrollmentDto;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.payment.service.VietQr;
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
class PaymentHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";}String api(){return "https://backend.example";}boolean secure(){return true;}
    void actor(Role role,boolean locked) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        var a=new AccountSnapshot(1L,role,locked,role!=Role.STUDENT,true);
        when(accounts.findById(1L)).thenReturn(Optional.of(a));when(credentials.lockAccountById(1L)).thenReturn(Optional.of(a));
    }
    PaymentDto payment() {return new PaymentDto(new ApiId(7),new ApiId(3),"PENDING","VND","100.5","0","0","0.5","101",
        "ELP0123456789ABCDEF012345",new ApiId(8),"970436","Bank","00123456","Teacher",
        VietQr.payload("970436","00123456","101","ELP0123456789ABCDEF012345"),now.plusSeconds(900),now);}
    void eligible() {
        actor(Role.STUDENT,false);when(credentials.lockAccountById(9L)).thenReturn(Optional.of(new AccountSnapshot(9L,Role.TEACHER,false,true,false)));
        when(paymentRepository.context(1,3)).thenReturn(Optional.of(new PaymentRepository.Context(2,9)));
        when(enrollmentRepository.course(2,true)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,"PUBLISHED",new BigDecimal("100.50"),1)));
        when(enrollmentRepository.firstStart(2)).thenReturn(now.plusSeconds(3600));when(enrollmentRepository.occupied(2,now)).thenReturn(1L);
        when(enrollmentRepository.forCourse(1,2,now)).thenReturn(Optional.of(new EnrollmentDto(new ApiId(3),new ApiId(2),"Course","PENDING",null,now.plusSeconds(900),false,null,null,now,now)));
        when(paymentRepository.destination(9)).thenReturn(Optional.of(new PaymentRepository.Destination(8,"970436","Bank","00123456","Teacher")));
        when(paymentRepository.price(2)).thenReturn(new PaymentRepository.Price(new BigDecimal("100.50"),"VND",null,null,null));
        when(paymentRepository.insert(eq(3L),any(),any(),anyString(),anyString(),eq(now.plusSeconds(900)),eq(now))).thenReturn(7L);
        when(paymentRepository.own(1,7)).thenReturn(Optional.of(payment()));
    }
    @Test void studentCreationHasNoStoreAndPendingOnly()throws Exception {
        eligible();mvc.perform(post("/api/v1/student/enrollments/3/payments").header("Authorization","Bearer access"))
            .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.finalAmount").value("101"))
            .andExpect(jsonPath("$.meetUrl").doesNotExist()).andExpect(jsonPath("$.qrObject").doesNotExist());
        verify(enrollmentRepository,never()).renew(anyLong(),anyLong(),any(),any());
    }
    @Test void latestPaymentRecoversOnlyAnOwnedEnrollment()throws Exception {
        actor(Role.STUDENT,false);
        String path="/api/v1/student/enrollments/3/payments/latest";
        mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isNotFound());
        verify(paymentRepository,never()).latestId(anyLong(),anyLong());
        when(enrollmentRepository.own(1,3,now)).thenReturn(Optional.of(new EnrollmentDto(new ApiId(3),new ApiId(2),"Course","PENDING",null,now.plusSeconds(900),false,null,null,now,now)));
        mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isNoContent());
        when(paymentRepository.latestId(1,3)).thenReturn(Optional.of(7L));
        mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store")).andExpect(content().json("{\"id\":\"7\"}"));
    }
    @Test void duplicateReturnsExistingAndDoesNotInsert()throws Exception {
        eligible();when(paymentRepository.pending(1,3)).thenReturn(Optional.of(payment()));
        mvc.perform(post("/api/v1/student/enrollments/3/payments").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("7"));
        verify(paymentRepository,never()).insert(anyLong(),any(),any(),anyString(),anyString(),any(),any());
    }
    @Test void rolesLockedAndAnonymousCannotAccess()throws Exception {
        mvc.perform(get("/api/v1/student/payments/7")).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.ADMIN,Role.TEACHER)) {
            actor(role,false);mvc.perform(post("/api/v1/student/enrollments/3/payments").header("Authorization","Bearer access")).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/student/payments/7/qr-image").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        actor(Role.STUDENT,true);mvc.perform(get("/api/v1/student/payments/7").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        verifyNoInteractions(paymentRepository);
    }
    @Test void ownershipAndClientAmountsRejected()throws Exception {
        actor(Role.STUDENT,false);
        mvc.perform(get("/api/v1/student/payments/999").header("Authorization","Bearer access")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/student/enrollments/999/payments").header("Authorization","Bearer access")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/student/enrollments/3/payments").header("Authorization","Bearer access").contentType("application/json").content("{\"amount\":1}"))
            .andExpect(status().isBadRequest());
    }
    @Test void accountUnavailableAndDatabaseErrorsAreSanitized()throws Exception {
        eligible();when(paymentRepository.destination(9)).thenReturn(Optional.empty());
        mvc.perform(post("/api/v1/student/enrollments/3/payments").header("Authorization","Bearer access")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RECEIVING_ACCOUNT_UNAVAILABLE"));
        when(paymentRepository.own(1,7)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private credentials"));
        mvc.perform(get("/api/v1/student/payments/7").header("Authorization","Bearer access")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("Payment request could not be completed."));
    }
    @Test void privatePngAndNoConfirmationEndpoint()throws Exception {
        eligible();mvc.perform(get("/api/v1/student/payments/7/qr-image").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(post("/api/v1/student/payments/7/confirm").header("Authorization","Bearer access")).andExpect(status().isForbidden());
    }
}

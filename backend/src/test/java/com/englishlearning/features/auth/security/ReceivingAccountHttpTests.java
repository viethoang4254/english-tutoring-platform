package com.englishlearning.features.auth.security;
import com.englishlearning.features.paymentaccount.dto.*;
import com.englishlearning.features.paymentaccount.repository.ReceivingAccountRepository;
import com.englishlearning.features.paymentaccount.service.*;
import com.englishlearning.features.paymentaccount.ReceivingImageTests;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.common.api.ApiId;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@org.springframework.boot.test.context.SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=false","app.security.browser.allowed-origins=https://frontend.example"})
class ReceivingAccountHttpTests extends AuthenticationHttpTestSupport {
    @org.springframework.test.context.bean.override.mockito.MockitoBean ReceivingAccountTransactions receivingTransactions;
    String origin(){return "https://frontend.example";}String api(){return "https://backend.example";}boolean secure(){return true;}
    void actor(Role role,boolean verified,boolean locked){when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());var a=new AccountSnapshot(1L,role,locked,verified,true, role == Role.TEACHER && verified);when(accounts.findById(1L)).thenReturn(Optional.of(a));when(credentials.lockAccountById(1L)).thenReturn(Optional.of(a));}
    ReceivingAccountRepository.Stored row(long owner){return new ReceivingAccountRepository.Stored(owner,new ReceivingAccount(new ApiId(9),"970436","VCB","Bank","Teacher","0012345678","PENDING","INACTIVE",true,now),"12345678-1234-1234-1234-123456789abc.png",QrImage.hash(ReceivingImageTests.png()),"fingerprint");}
    @Test void anonymousStudentUnverifiedAndLockedDenied()throws Exception {
        mvc.perform(get("/api/v1/teacher/payment-accounts")).andExpect(status().isUnauthorized());
        actor(Role.STUDENT,true,false);mvc.perform(get("/api/v1/teacher/payment-accounts").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        actor(Role.TEACHER,false,false);mvc.perform(get("/api/v1/teacher/payment-accounts").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        actor(Role.TEACHER,true,true);mvc.perform(get("/api/v1/teacher/payment-accounts").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());verifyNoInteractions(receivingRepository,qrStorage);
    }
    @Test void listMasksNumbersAndCrossOwnerIsNotFound()throws Exception {
        actor(Role.TEACHER,true,false);when(receivingRepository.list(1,0,20)).thenReturn(List.of(row(1)));when(receivingRepository.get(9)).thenReturn(Optional.of(row(2)));
        mvc.perform(get("/api/v1/teacher/payment-accounts").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].accountNumber").value("****5678")).andExpect(jsonPath("$.items[0].qrObject").doesNotExist());
        mvc.perform(get("/api/v1/teacher/payment-accounts/9/qr-image").header("Authorization","Bearer access")).andExpect(status().isNotFound());verifyNoInteractions(qrStorage);
    }
    @Test void onlyOwnerAndVerifiedAdminCanViewImageWithoutPaymentAuthority()throws Exception {
        when(receivingRepository.get(9)).thenReturn(Optional.of(row(1)));when(qrStorage.read(anyString())).thenReturn(ReceivingImageTests.png());actor(Role.TEACHER,true,false);
        mvc.perform(get("/api/v1/teacher/payment-accounts/9/qr-image").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff"));
        actor(Role.ADMIN,true,false);mvc.perform(get("/api/v1/admin/payment-accounts/9/qr-image").header("Authorization","Bearer access")).andExpect(status().isOk());
        for(String path:List.of("/api/v1/teacher/courses/2/payments/9/confirm-manually","/api/v1/student/enrollments/9/activate"))mvc.perform(post(path).header("Authorization","Bearer access")).andExpect(status().isForbidden());
    }
    @Test void submissionIsPendingAndRejectsAuthorityFields()throws Exception {
        actor(Role.TEACHER,true,false);
        when(receivingTransactions.persist(any(),any())).thenReturn(new ReceivingAccountService.Created(row(1).account(),true));
        var image=new MockMultipartFile("qrImage","qr.png","image/png",ReceivingImageTests.png());
        mvc.perform(multipart("/api/v1/teacher/payment-accounts").file(image).param("bankBin","970436").param("holderName","Teacher").param("accountNumber","0012345678").header("Idempotency-Key",UUID.randomUUID().toString()).header("Authorization","Bearer access"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.reviewStatus").value("PENDING")).andExpect(jsonPath("$.selectionStatus").value("INACTIVE"));
        verify(receivingTransactions).persist(argThat(input -> input.actor()==1L && input.bank().bin().equals("970436")
            && input.bank().code().equals("VCB") && input.holder().equals("Teacher") && input.number().equals("0012345678")
            && input.imageSize()>0 && input.fingerprint().matches("[0-9a-f]{64}")),any());
        mvc.perform(multipart("/api/v1/teacher/payment-accounts").file(image).param("bankBin","970436").param("holderName","Teacher").param("accountNumber","0012345678").param("teacherId","2").header("Idempotency-Key",UUID.randomUUID().toString()).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
    }
    @Test void pendingAndRejectedEligibilityAndStrictPagination()throws Exception {
        actor(Role.TEACHER,true,false);doThrow(new com.englishlearning.features.teacher.service.TeacherReviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
        mvc.perform(get("/api/v1/teacher/payment-accounts").header("Authorization","Bearer access")).andExpect(status().isForbidden());verifyNoInteractions(receivingRepository);
        doNothing().when(teacherEligibility).requireEligible(1L);
        for(String q:List.of("size=101","page=-1","page=0&page=1","teacherId=2"))mvc.perform(get("/api/v1/teacher/payment-accounts?"+q).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
    }
    @Test void corsAllowsExplicitIdempotencyHeaderOnlyForConfiguredOrigin()throws Exception {
        mvc.perform(options("/api/v1/teacher/payment-accounts").header("Origin",origin()).header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","authorization,content-type,idempotency-key")).andExpect(status().isOk());
        mvc.perform(options("/api/v1/teacher/payment-accounts").header("Origin","https://evil.invalid").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","idempotency-key")).andExpect(status().isForbidden());
    }
    @Test void malformedImageAndStorageFailureNeverPersistAccount()throws Exception {
        actor(Role.TEACHER,true,false);
        var invalid=new MockMultipartFile("qrImage","qr.png","image/png","not an image".getBytes());
        mvc.perform(multipart("/api/v1/teacher/payment-accounts").file(invalid).param("bankBin","970436").param("holderName","Teacher").param("accountNumber","0012345678").header("Idempotency-Key",UUID.randomUUID().toString()).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        verifyNoInteractions(qrStorage);
        doThrow(new ReceivingFailure(ReceivingFailure.Reason.UNAVAILABLE)).when(qrStorage).put(anyString(),any());
        var valid=new MockMultipartFile("qrImage","qr.png","image/png",ReceivingImageTests.png());
        mvc.perform(multipart("/api/v1/teacher/payment-accounts").file(valid).param("bankBin","970436").param("holderName","Teacher").param("accountNumber","0012345678").header("Idempotency-Key",UUID.randomUUID().toString()).header("Authorization","Bearer access")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("RECEIVING_ACCOUNT_UNAVAILABLE"));
        verify(receivingTransactions,never()).persist(any(),any());
        verify(receivingRepository,never()).insert(anyLong(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyString(),anyInt(),any(),anyString(),any());
    }

}

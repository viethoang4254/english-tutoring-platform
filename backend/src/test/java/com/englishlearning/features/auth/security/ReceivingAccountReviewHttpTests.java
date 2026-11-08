package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.paymentaccount.dto.*;
import com.englishlearning.features.paymentaccount.service.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.context.SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=false","app.security.browser.allowed-origins=https://frontend.example"})
class ReceivingAccountReviewHttpTests extends AuthenticationHttpTestSupport {
    @MockitoBean ReceivingAccountReviewService reviews;
    String origin(){return "https://frontend.example";}String api(){return "https://backend.example";}boolean secure(){return true;}
    void actor(Role role,boolean verified,boolean locked) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("2").build());
        when(accounts.findById(2L)).thenReturn(Optional.of(new AccountSnapshot(2L,role,locked,verified,true)));
    }
    @Test void adminPendingAndDetailsArePrivateAndDoNotExposeObjectPaths()throws Exception {
        actor(Role.ADMIN,true,false);
        when(reviews.pending(2,0,20)).thenReturn(new ReceivingAccountReviewService.Page(List.of(new ReceivingAccountReviewService.Item(new ApiId(9),new ApiId(1),"Bank",now)),0,20,false));
        var account=new ReceivingAccount(new ApiId(9),"970436","VCB","Bank","Teacher","0012345678","PENDING","INACTIVE",true,now);
        when(reviews.detail(2,9)).thenReturn(new ReceivingAccountReview(new ApiId(1),account,null,null,null));
        mvc.perform(get("/api/v1/admin/payment-accounts/pending").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.items[0].teacherId").value("1")).andExpect(jsonPath("$.items[0].accountNumber").doesNotExist());
        mvc.perform(get("/api/v1/admin/payment-accounts/9").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.account.accountNumber").value("0012345678")).andExpect(jsonPath("$.account.qrObject").doesNotExist());
    }
    @Test void approveAndRejectUseAuthenticatedReviewer()throws Exception {
        actor(Role.ADMIN,true,false);
        mvc.perform(post("/api/v1/admin/payment-accounts/9/approve").header("Authorization","Bearer access")).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/admin/payment-accounts/10/reject").contentType("application/json").content("{\"reason\":\"Details mismatch\"}").header("Authorization","Bearer access")).andExpect(status().isNoContent());
        verify(reviews).review(2,9,true,null);verify(reviews).review(2,10,false,"Details mismatch");
        verifyNoInteractions(receivingRepository,qrStorage);
    }
    @Test void anonymousStudentTeacherLockedAndUnverifiedAdminDenied()throws Exception {
        mvc.perform(post("/api/v1/admin/payment-accounts/9/approve")).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.STUDENT,Role.TEACHER)) {
            actor(role,true,false);
            mvc.perform(get("/api/v1/admin/payment-accounts/pending").header("Authorization","Bearer access")).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/payment-accounts/9/approve").header("Authorization","Bearer access")).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/payment-accounts/9/reject").contentType("application/json").content("{\"reason\":\"Mismatch\"}").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        actor(Role.ADMIN,false,false);mvc.perform(post("/api/v1/admin/payment-accounts/9/approve").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        actor(Role.ADMIN,true,true);mvc.perform(post("/api/v1/admin/payment-accounts/9/approve").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        verifyNoInteractions(reviews);
    }
    @Test void rejectsMissingReasonAuthorityFieldsBodiesAndInvalidPagination()throws Exception {
        actor(Role.ADMIN,true,false);
        for(String body:List.of("{}","{\"reason\":\" \"}","{\"reason\":\"Mismatch\",\"reviewedBy\":1}"))
            mvc.perform(post("/api/v1/admin/payment-accounts/9/reject").contentType("application/json").content(body).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/payment-accounts/9/approve").contentType("application/json").content("{}").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        for(String query:List.of("size=101","page=-1","page=0&page=1","teacherId=1"))
            mvc.perform(get("/api/v1/admin/payment-accounts/pending?"+query).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        verifyNoInteractions(reviews);
    }
    @Test void duplicateDecisionsReturnConflictAndUnknownAccountsReturn404()throws Exception {
        actor(Role.ADMIN,true,false);
        doThrow(new ReceivingFailure(ReceivingFailure.Reason.ALREADY_REVIEWED)).when(reviews).review(2,9,true,null);
        when(reviews.detail(2,99)).thenThrow(new ReceivingFailure(ReceivingFailure.Reason.MISSING));
        mvc.perform(post("/api/v1/admin/payment-accounts/9/approve").header("Authorization","Bearer access")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RECEIVING_ACCOUNT_ALREADY_REVIEWED"));
        mvc.perform(get("/api/v1/admin/payment-accounts/99").header("Authorization","Bearer access")).andExpect(status().isNotFound());
    }
    @Test void adminStillCannotConfirmPaymentsActivateEnrollmentOrSwitchAccounts()throws Exception {
        actor(Role.ADMIN,true,false);
        for(String path:List.of("/api/v1/admin/payment-accounts/9/activate","/api/v1/teacher/courses/2/payments/9/confirm-manually","/api/v1/student/enrollments/9/activate"))
            mvc.perform(post(path).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        verifyNoInteractions(reviews,receivingRepository,qrStorage);
    }
}

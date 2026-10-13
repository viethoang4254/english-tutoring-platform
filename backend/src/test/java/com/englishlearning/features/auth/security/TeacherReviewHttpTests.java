package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.teacher.dto.TeacherApplicationDetail;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;

@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false", "app.security.browser.local-http=true",
        "app.security.browser.allowed-origins=http://localhost:3000"})
class TeacherReviewHttpTests extends AuthenticationHttpTestSupport {
    static final String BASE="/api/v1/admin/teacher-applications/9007199254740993";
    String origin(){return "http://localhost:3000";}
    String api(){return "http://localhost:8080";}
    boolean secure(){return false;}
    @BeforeEach void admin() {
        when(decoder.decode("admin-token")).thenReturn(Jwt.withTokenValue("admin-token").header("alg","RS256")
                .subject("12").issuedAt(now).expiresAt(now.plusSeconds(900)).build());
        when(accounts.findById(12L)).thenReturn(Optional.of(new AccountSnapshot(12L,Role.ADMIN,false,true)));
    }
    @Test void safeExactDetailProjectionAndDecimalIds() throws Exception {
        when(teacherReviews.detail(12L,9007199254740993L)).thenReturn(new TeacherApplicationDetail(
                new ApiId(9007199254740993L),new ApiId(22L)," English ","Experience","Introduction",
                "REJECTED",now,new ApiId(12L),now," reason "));
        var response=mvc.perform(get(BASE).header("Authorization","Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.id").value("9007199254740993"))
                .andExpect(jsonPath("$.teacherId").value("22"))
                .andExpect(jsonPath("$.reviewedBy").value("12")).andReturn().getResponse();
        var fields=mapper.readTree(response.getContentAsString());
        assertEquals(Set.of("id","teacherId","specialization","teachingExperience","introduction","status",
                "submittedAt","reviewedBy","reviewedAt","rejectionReason"),fields.propertyNames());
    }
    @Test void approveAndRejectUseAuthenticatedReviewer() throws Exception {
        mvc.perform(post(BASE+"/approve").header("Authorization","Bearer admin-token"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(teacherReviews).review(12L,9007199254740993L,true,null);
        mvc.perform(post(BASE+"/reject").header("Authorization","Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" reason \"}"))
                .andExpect(status().isNoContent());
        verify(teacherReviews).review(12L,9007199254740993L,false," reason ");
    }
    @Test void reviewerRoleVerificationAndLockRestrictions() throws Exception {
        for(var state:List.of(new AccountSnapshot(12L,Role.STUDENT,false,true),
                new AccountSnapshot(12L,Role.TEACHER,false,true),new AccountSnapshot(12L,Role.ADMIN,false,false),
                new AccountSnapshot(12L,Role.ADMIN,true,true))) {
            when(accounts.findById(12L)).thenReturn(Optional.of(state));
            for(String suffix:List.of("","/approve","/reject")) {
                var request=suffix.isEmpty()?get(BASE):post(BASE+suffix);
                mvc.perform(request.header("Authorization","Bearer admin-token"))
                        .andExpect(status().is(state.locked()?401:403));
            }
        }
        verifyNoInteractions(teacherReviews);
    }
    @Test void authenticationPrecedesInvalidInput() throws Exception {
        mvc.perform(post(BASE+"/approve").content("bad")).andExpect(status().isUnauthorized());
        when(decoder.decode("bad")).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("private"));
        mvc.perform(get(BASE).header("Authorization","Bearer bad")).andExpect(status().isUnauthorized());
        when(accounts.findById(12L)).thenReturn(Optional.empty());
        mvc.perform(get(BASE).header("Authorization","Bearer admin-token")).andExpect(status().isUnauthorized());
        verifyNoInteractions(teacherReviews);
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"reason\":null}","{\"reason\":\" \"}",
            "{\"reason\":1}","{\"reason\":true}","{\"reason\":[]}","{\"reason\":\"bad\\u0000text\"}",
            "{\"reason\":\"ok\",\"reviewedBy\":\"1\"}","{\"reason\":\"ok\",\"status\":\"APPROVED\"}"})
    void rejectsInvalidAndProtectedBody(String body) throws Exception {
        mvc.perform(post(BASE+"/reject").header("Authorization","Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(teacherReviews);
    }
    @Test void rejectsQueryBodyAndInvalidIdsWithoutMutation() throws Exception {
        mvc.perform(post(BASE+"/approve").header("Authorization","Bearer admin-token").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(BASE+"/approve").header("Authorization","Bearer admin-token").queryParam("role","ADMIN"))
                .andExpect(status().isBadRequest());
        for(String id:List.of("0","-1","1.5","9223372036854775808"))
            mvc.perform(get("/api/v1/admin/teacher-applications/"+id).header("Authorization","Bearer admin-token"))
                    .andExpect(status().isBadRequest());
        verifyNoInteractions(teacherReviews);
    }
    @Test void safeBusinessAndServerErrors() throws Exception {
        var reasons=new TeacherReviewFailure.Reason[]{TeacherReviewFailure.Reason.NOT_FOUND,
                TeacherReviewFailure.Reason.ALREADY_REVIEWED,TeacherReviewFailure.Reason.PROFILE_EXISTS};
        int[] statuses={404,409,409};String[] codes={"APPLICATION_NOT_FOUND","APPLICATION_ALREADY_REVIEWED","TEACHER_PROFILE_CONFLICT"};
        for(int i=0;i<reasons.length;i++) {
            doThrow(new TeacherReviewFailure(reasons[i])).when(teacherReviews).review(12L,9007199254740993L,true,null);
            mvc.perform(post(BASE+"/approve").header("Authorization","Bearer admin-token"))
                    .andExpect(status().is(statuses[i])).andExpect(jsonPath("$.code").value(codes[i]));
        }
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("private SQL/password"))
                .when(teacherReviews).review(12L,9007199254740993L,true,null);
        mvc.perform(post(BASE+"/approve").header("Authorization","Bearer admin-token"))
                .andExpect(status().isInternalServerError()).andExpect(content().json(
                        "{\"code\":\"INTERNAL_ERROR\",\"message\":\"An unexpected error occurred.\"}",org.springframework.test.json.JsonCompareMode.STRICT));
    }
    @Test void noQueueOrOtherProductionPermission() throws Exception {
        for(String path:List.of("/api/v1/admin/teacher-applications","/api/v1/teacher/courses","/api/v1/teacher/application"))
            mvc.perform(get(path).header("Authorization","Bearer admin-token")).andExpect(status().isForbidden());
    }
}

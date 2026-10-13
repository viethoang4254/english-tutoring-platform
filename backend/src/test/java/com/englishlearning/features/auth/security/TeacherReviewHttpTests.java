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
    static final String QUEUE="/api/v1/admin/teacher-applications/pending";
    @Test void pendingQueueUsesDefaultsAndSafeProjection() throws Exception {
        var item=new com.englishlearning.features.teacher.dto.TeacherApplicationQueue.Item(
                new ApiId(9007199254740993L),new ApiId(9007199254740994L),"PENDING",now);
        when(teacherReviews.pending(12L,0,20)).thenReturn(
                new com.englishlearning.features.teacher.dto.TeacherApplicationQueue(List.of(item),0,20,true));
        var response=mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.items[0].id").value("9007199254740993"))
                .andExpect(jsonPath("$.items[0].teacherId").value("9007199254740994"))
                .andExpect(jsonPath("$.items[0].submittedAt").value(now.toString()))
                .andExpect(jsonPath("$.hasNext").value(true)).andReturn().getResponse();
        var json=mapper.readTree(response.getContentAsString());
        assertEquals(Set.of("items","page","size","hasNext"),json.propertyNames());
        assertEquals(Set.of("id","teacherId","status","submittedAt"),json.get("items").get(0).propertyNames());
        verify(teacherReviews).pending(12L,0,20);
    }
    @Test void pendingQueueSupportsExplicitPaginationAndEmptyResults() throws Exception {
        when(teacherReviews.pending(12L,2,100)).thenReturn(
                new com.englishlearning.features.teacher.dto.TeacherApplicationQueue(List.of(),2,100,false));
        mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").queryParam("page","2").queryParam("size","100"))
                .andExpect(status().isOk()).andExpect(content().json(
                        "{\"items\":[],\"page\":2,\"size\":100,\"hasNext\":false}",org.springframework.test.json.JsonCompareMode.STRICT));
    }
    @ParameterizedTest @ValueSource(strings={"","-1","+1","1.5"," 1","1 ","true","2147483648","999999999999999999999"})
    void pendingQueueRejectsInvalidIntegers(String value) throws Exception {
        for(String field:List.of("page","size"))
            mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").queryParam(field,value))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(teacherReviews);
    }
    @Test void pendingQueueRejectsDuplicateUnknownBodyRangeAndOffset() throws Exception {
        for(String field:List.of("page","size"))
            mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").queryParam(field,"1","2"))
                    .andExpect(status().isBadRequest());
        for(String field:List.of("status","sort","teacherId"))
            mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").queryParam(field,"1"))
                    .andExpect(status().isBadRequest());
        for(String size:List.of("0","101"))
            mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").queryParam("size",size))
                    .andExpect(status().isBadRequest());
        mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").queryParam("page","2147483647"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(teacherReviews);
    }
    @Test void pendingQueueRequiresEligibleAdminBeforeInputValidation() throws Exception {
        for(var state:List.of(new AccountSnapshot(12L,Role.STUDENT,false,true),
                new AccountSnapshot(12L,Role.TEACHER,false,true),new AccountSnapshot(12L,Role.ADMIN,false,false),
                new AccountSnapshot(12L,Role.ADMIN,true,true))) {
            when(accounts.findById(12L)).thenReturn(Optional.of(state));
            mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token").queryParam("size","bad"))
                    .andExpect(status().is(state.locked()?401:403));
        }
        mvc.perform(get(QUEUE).queryParam("size","bad")).andExpect(status().isUnauthorized());
        when(accounts.findById(12L)).thenReturn(Optional.empty());
        mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token")).andExpect(status().isUnauthorized());
        verifyNoInteractions(teacherReviews);
    }
    @Test void pendingQueueFailuresAreSafe() throws Exception {
        when(teacherReviews.pending(12L,0,20)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE SQL"));
        mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token"))
                .andExpect(status().isInternalServerError()).andExpect(content().json(
                        "{\"code\":\"INTERNAL_ERROR\",\"message\":\"An unexpected error occurred.\"}",org.springframework.test.json.JsonCompareMode.STRICT));
        when(accounts.findById(12L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE SQL"));
        mvc.perform(get(QUEUE).header("Authorization","Bearer admin-token"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AUTHENTICATION_UNAVAILABLE"));
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

package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.teacher.dto.TeacherOnboardingStatus;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;

@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false","app.security.browser.local-http=true",
        "app.security.browser.allowed-origins=http://localhost:3000"})
class TeacherOnboardingHttpTests extends AuthenticationHttpTestSupport {
    static final String PATH="/api/v1/teacher/onboarding";
    String origin(){return "http://localhost:3000";}
    String api(){return "http://localhost:8080";}
    boolean secure(){return false;}
    @BeforeEach void teacher() {
        when(decoder.decode("teacher-token")).thenReturn(Jwt.withTokenValue("teacher-token").header("alg","RS256")
                .subject("22").issuedAt(now).expiresAt(now.plusSeconds(900)).build());
        when(accounts.findById(22L)).thenReturn(Optional.of(new AccountSnapshot(22L,Role.TEACHER,false,true)));
    }
    @ParameterizedTest @ValueSource(strings={"PENDING","APPROVED","REJECTED"})
    void exactSafeProjectionForEveryStatus(String state) throws Exception {
        when(teacherOnboarding.ownStatus(22L)).thenReturn(new TeacherOnboardingStatus(new ApiId(9007199254740993L),state,
                now,"PENDING".equals(state)?null:now,"REJECTED".equals(state)?" reason ":null));
        var response=mvc.perform(get(PATH).header("Authorization","Bearer teacher-token"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.applicationId").value("9007199254740993"))
                .andExpect(jsonPath("$.status").value(state)).andReturn().getResponse();
        var json=mapper.readTree(response.getContentAsString());
        assertEquals(Set.of("applicationId","status","submittedAt","reviewedAt","rejectionReason"),json.propertyNames());
        assertEquals("PENDING".equals(state),json.get("reviewedAt").isNull());
        if ("REJECTED".equals(state)) assertEquals(" reason ",json.get("rejectionReason").asString());
        else assertTrue(json.get("rejectionReason").isNull());
        verify(teacherOnboarding).ownStatus(22L);verifyNoInteractions(teacherEligibility);
    }
    @Test void onlyAuthenticatedTeacherCanSelectOwnStatus() throws Exception {
        for(var state:List.of(new AccountSnapshot(22L,Role.STUDENT,false,true),
                new AccountSnapshot(22L,Role.ADMIN,false,true),new AccountSnapshot(22L,Role.TEACHER,false,false),
                new AccountSnapshot(22L,Role.TEACHER,true,true))) {
            when(accounts.findById(22L)).thenReturn(Optional.of(state));
            mvc.perform(get(PATH).header("Authorization","Bearer teacher-token"))
                    .andExpect(status().is(state.locked()?401:403));
        }
        when(accounts.findById(22L)).thenReturn(Optional.empty());
        mvc.perform(get(PATH).header("Authorization","Bearer teacher-token")).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        verifyNoInteractions(teacherOnboarding);
    }
    @Test void clientIdentityAndBodiesAreRejectedAfterAuthentication() throws Exception {
        for(String name:List.of("userId","applicationId","teacherId"))
            mvc.perform(get(PATH).header("Authorization","Bearer teacher-token").queryParam(name,"999"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get(PATH).header("Authorization","Bearer teacher-token").content("{\"userId\":999}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(PATH).queryParam("userId","999")).andExpect(status().isUnauthorized());
        verifyNoInteractions(teacherOnboarding);
    }
    @Test void missingApplicationHasDistinctSafe404() throws Exception {
        when(teacherOnboarding.ownStatus(22L)).thenThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.ONBOARDING_NOT_FOUND));
        mvc.perform(get(PATH).header("Authorization","Bearer teacher-token"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ONBOARDING_APPLICATION_NOT_FOUND"));
    }
    @Test void failuresDoNotLeakPrivateData() throws Exception {
        when(teacherOnboarding.ownStatus(22L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE SQL/password"));
        mvc.perform(get(PATH).header("Authorization","Bearer teacher-token"))
                .andExpect(status().isInternalServerError()).andExpect(content().json(
                        "{\"code\":\"INTERNAL_ERROR\",\"message\":\"An unexpected error occurred.\"}",org.springframework.test.json.JsonCompareMode.STRICT));
    }
    @Test void routeDoesNotGrantBusinessOrMutationPermission() throws Exception {
        mvc.perform(post(PATH).header("Authorization","Bearer teacher-token")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/teacher/courses").header("Authorization","Bearer teacher-token")).andExpect(status().isForbidden());
        verifyNoInteractions(teacherOnboarding);
    }
}

package com.englishlearning.features.auth.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.enrollment.service.EnrollmentService;
import com.englishlearning.features.course.service.CourseInput;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;

class MeetingHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";} String api(){return "https://backend.example";} boolean secure(){return true;}
    final String student="/api/v1/student/sessions/42/join", teacher="/api/v1/teacher/courses/42/meeting";
    final String room="https://meet.google.com/abc-defg-hij";
    void account(Role role,boolean locked,boolean approved) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,role==Role.ADMIN,role==Role.STUDENT,approved)));
    }
    @Test void studentUsesIdentityAndReadOnlyNoStoreResponse() throws Exception {
        account(Role.STUDENT,false,false);when(enrollmentRepository.sessionMeeting(1,42)).thenReturn(Optional.of(room));
        mvc.perform(get(student).header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store")).andExpect(content().json("{\"meetUrl\":\""+room+"\"}",org.springframework.test.json.JsonCompareMode.STRICT));
        verify(enrollmentRepository).sessionMeeting(1,42);verifyNoMoreInteractions(enrollmentRepository);
        assertTrue(EnrollmentService.class.getMethod("sessionMeeting",long.class,long.class).getAnnotation(Transactional.class).readOnly());
    }
    @Test void absentOrInvalidRoomNeverLeaks() throws Exception {
        account(Role.STUDENT,false,false);
        for(String value:List.of("", "https://meet.google.com.evil.test/abc-defg-hij", "http://meet.google.com/abc-defg-hij",room+"?secret=x",room+"/", "javascript:alert(1)")) {
            when(enrollmentRepository.sessionMeeting(1,42)).thenReturn(value.isEmpty()?Optional.empty():Optional.of(value));
            mvc.perform(get(student).header("Authorization","Bearer access")).andExpect(status().isForbidden()).andExpect(jsonPath("$.meetUrl").doesNotExist());
            assertFalse(CourseInput.validMeetUrl(value));
        }
        assertFalse(CourseInput.validMeetUrl(null));assertTrue(CourseInput.validMeetUrl(room));
    }
    @Test void studentRoleLockAndInputBoundaries() throws Exception {
        mvc.perform(get(student)).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.TEACHER,Role.ADMIN)) {
            account(role,false,true);mvc.perform(get(student).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        account(Role.STUDENT,true,false);mvc.perform(get(student).header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        account(Role.STUDENT,false,false);
        mvc.perform(get(student+"?studentId=2").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get(student).content("{}").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/student/sessions/0/join").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        verifyNoInteractions(enrollmentRepository);
    }
    @Test void teacherOwnerReadbackAndMissingRoom() throws Exception {
        account(Role.TEACHER,false,true);when(courseRepository.ownedMeeting(1,42)).thenReturn(Optional.of(room));
        mvc.perform(get(teacher).header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.meetUrl").value(room));
        verify(teacherEligibility).requireEligible(1L);
        for(String value:List.of("", "https://evil.test/abc-defg-hij")) {
            when(courseRepository.ownedMeeting(1,42)).thenReturn(Optional.of(value));
            mvc.perform(get(teacher).header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(content().json("{}",org.springframework.test.json.JsonCompareMode.STRICT));
        }
        when(courseRepository.ownedMeeting(1,42)).thenReturn(Optional.empty());
        mvc.perform(get(teacher).header("Authorization","Bearer access")).andExpect(status().isNotFound());
    }
    @Test void teacherApprovalRolesLockAndInputBoundaries() throws Exception {
        mvc.perform(get(teacher)).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.STUDENT,Role.ADMIN)) {
            account(role,false,false);mvc.perform(get(teacher).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        // Verified but not approved covers both pending and rejected onboarding.
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true,false,false)));
        mvc.perform(get(teacher).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        account(Role.TEACHER,true,true);mvc.perform(get(teacher).header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        account(Role.TEACHER,false,true);
        mvc.perform(get(teacher+"?teacherId=2").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        verifyNoInteractions(courseRepository);
    }
    @Test void infrastructureErrorsAreSanitized() throws Exception {
        account(Role.STUDENT,false,false);
        when(enrollmentRepository.sessionMeeting(1,42)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE"));
        mvc.perform(get(student).header("Authorization","Bearer access")).andExpect(status().isServiceUnavailable())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE"))));
    }
}

package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.ApiId;
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
class EnrollmentHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";} String api(){return "https://backend.example";} boolean secure(){return true;}
    void actor(Role role,boolean verified,boolean locked) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        var a=new AccountSnapshot(1L,role,locked,verified,true);when(accounts.findById(1L)).thenReturn(Optional.of(a));when(credentials.lockAccountById(1L)).thenReturn(Optional.of(a));
    }
    EnrollmentDto row(){return new EnrollmentDto(new ApiId(9007199254740993L),new ApiId(2),"Course","PENDING",null,now.plusSeconds(900),false,null,null,now,now);}
    @Test void verifiedAndUnverifiedStudentCanCreateWithoutActivation() throws Exception {
        for(boolean verified:new boolean[]{false,true}) {
            actor(Role.STUDENT,verified,false);
            when(enrollmentRepository.course(2,true)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,"PUBLISHED",BigDecimal.ONE,1)));
            when(enrollmentRepository.firstStart(2)).thenReturn(now.plusSeconds(3600));when(enrollmentRepository.create(eq(1L),eq(2L),any(),eq(now))).thenReturn(9007199254740993L);
            when(enrollmentRepository.own(1,9007199254740993L,now)).thenReturn(Optional.of(row()));
            mvc.perform(post("/api/v1/student/courses/2/enrollment").header("Authorization","Bearer access"))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.id").value("9007199254740993")).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.meetUrl").doesNotExist()).andExpect(jsonPath("$.content").doesNotExist());
        }
    }
    @Test void anonymousWrongRoleAndLockedDenied() throws Exception {
        mvc.perform(get("/api/v1/student/enrollments")).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.ADMIN,Role.TEACHER)) {actor(role,true,false);mvc.perform(get("/api/v1/student/enrollments").header("Authorization","Bearer access")).andExpect(status().isForbidden());}
        actor(Role.STUDENT,true,true);mvc.perform(get("/api/v1/student/enrollments").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        verifyNoInteractions(enrollmentRepository);
    }
    @Test void strictQueriesBodiesAndCrossStudentDetail() throws Exception {
        actor(Role.STUDENT,false,false);
        for(String query:List.of("size=101","page=-1","status=EXPIRED","studentId=2","page=0&page=1"))
            mvc.perform(get("/api/v1/student/enrollments?"+query).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/student/courses/2/enrollment").header("Authorization","Bearer access").contentType("application/json").content("{\"studentId\":2}" )).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/student/enrollments/77").header("Authorization","Bearer access")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_FOUND"));
        mvc.perform(post("/api/v1/student/enrollments/77/renew").header("Authorization","Bearer access")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/student/enrollments/77/cancel").header("Authorization","Bearer access")).andExpect(status().isNotFound());
    }
    @Test void pendingCannotReachProtectedRoutesOrSetActive() throws Exception {
        actor(Role.STUDENT,false,false);
        mvc.perform(get("/api/v1/teacher/courses/2/sessions").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/student/courses/2/sessions").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/student/enrollments/3/activate").header("Authorization","Bearer access")).andExpect(status().isForbidden());
    }
    @Test void listPaginationAndTeacherRosterPrivacy() throws Exception {
        actor(Role.STUDENT,false,false);when(enrollmentRepository.list(1,0,1,null,now)).thenReturn(List.of(row(),row()));
        mvc.perform(get("/api/v1/student/enrollments?size=1").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.hasNext").value(true)).andExpect(jsonPath("$.items.length()").value(1));
        actor(Role.TEACHER,true,false);when(enrollmentRepository.course(2,false)).thenReturn(Optional.of(new EnrollmentRepository.Course(1,"PUBLISHED",BigDecimal.ONE,1)));
        when(enrollmentRepository.roster(1,2,0,20,null)).thenReturn(List.of(new EnrollmentRepository.RosterRow(new ApiId(3),new ApiId(7),"Student","PENDING",null)));
        mvc.perform(get("/api/v1/teacher/courses/2/enrollments").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].studentId").value("7")).andExpect(jsonPath("$.items[0].email").doesNotExist());
        mvc.perform(get("/api/v1/teacher/courses/99/enrollments").header("Authorization","Bearer access")).andExpect(status().isNotFound());
    }
    @Test void pendingRejectedTeachersAndUnavailablePersistenceFailSafely() throws Exception {
        actor(Role.TEACHER,true,false);
        doThrow(new com.englishlearning.features.teacher.service.TeacherReviewFailure(com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
        mvc.perform(get("/api/v1/teacher/courses/2/enrollments").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        verifyNoInteractions(enrollmentRepository);
        actor(Role.STUDENT,false,false);
        when(enrollmentRepository.list(1,0,20,null,now)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private database details"));
        mvc.perform(get("/api/v1/student/enrollments").header("Authorization","Bearer access")).andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("ENROLLMENT_UNAVAILABLE")).andExpect(jsonPath("$.message").value("Enrollment request could not be completed."));
    }
    @Test void renewCancelAndDuplicateUseExistingIdentity() throws Exception {
        actor(Role.STUDENT,false,false);var e=row();
        when(enrollmentRepository.course(2,true)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,"PUBLISHED",BigDecimal.ONE,1)));
        when(enrollmentRepository.own(1,e.id().value(),now)).thenReturn(Optional.of(e));
        when(enrollmentRepository.forCourse(1,2,now)).thenReturn(Optional.of(e));
        mvc.perform(post("/api/v1/student/courses/2/enrollment").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(e.id().decimal()));
        mvc.perform(post("/api/v1/student/enrollments/"+e.id().decimal()+"/renew").header("Authorization","Bearer access")).andExpect(status().isOk());
        verify(enrollmentRepository,never()).renew(anyLong(),anyLong(),any(),any());
        var cancelled=new EnrollmentDto(e.id(),e.courseId(),e.courseTitle(),"CANCELLED",null,null,false,now,null,now,now);
        when(enrollmentRepository.own(1,e.id().value(),now)).thenReturn(Optional.of(e),Optional.of(cancelled));
        mvc.perform(post("/api/v1/student/enrollments/"+e.id().decimal()+"/cancel").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        verify(enrollmentRepository).cancel(1,e.id().value(),now);
    }
}

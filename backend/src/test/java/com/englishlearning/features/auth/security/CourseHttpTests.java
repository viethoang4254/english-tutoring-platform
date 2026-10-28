package com.englishlearning.features.auth.security;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.common.api.*;
import com.englishlearning.features.category.dto.CategoryDto;
import com.englishlearning.features.course.CourseInputTests;
import com.englishlearning.features.course.dto.CourseDto;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

@org.springframework.boot.test.context.SpringBootTest(properties={
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "spring.flyway.enabled=false","app.security.browser.local-http=false","app.security.browser.allowed-origins=https://frontend.example"})
class CourseHttpTests extends AuthenticationHttpTestSupport {
    String origin() { return "https://frontend.example"; }
    String api() { return "https://backend.example"; }
    boolean secure() { return true; }
    void account(Role role, boolean verified, boolean locked) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        var a = new AccountSnapshot(1L, role, locked, verified, false);
        when(accounts.findById(1L)).thenReturn(Optional.of(a));
        when(credentials.lockAccountById(1L)).thenReturn(Optional.of(a));
    }
    CourseDto row(String status) {
        return new CourseDto(new ApiId(9007199254740993L), "English", status,
                new CourseDto.Category(new ApiId(1), "Category", false), new ApiMoney("0.00","VND"),
                LocalDate.of(2026,1,1), "Asia/Ho_Chi_Minh", 200, 1, 150, now, now, "Description", null);
    }
    @Test void createAndListUseSafeDtosAndPrincipal() throws Exception {
        account(Role.TEACHER,true,false);
        when(categoryRepository.find(1,true)).thenReturn(Optional.of(new CategoryDto(new ApiId(1),"Category",true,now,now)));
        when(courseRepository.create(eq(1L),any(),eq(now))).thenReturn(row("DRAFT"));
        mvc.perform(post("/api/v1/teacher/courses").header("Authorization","Bearer access").contentType("application/json")
                .content(mapper.writeValueAsString(CourseInputTests.fields())))
            .andExpect(status().isCreated()).andExpect(header().string("Location","/api/v1/teacher/courses/9007199254740993"))
            .andExpect(jsonPath("$.id").value("9007199254740993")).andExpect(jsonPath("$.meetUrl").doesNotExist());
        when(courseRepository.list(1,0,1,"DRAFT")).thenReturn(List.of(row("DRAFT"), row("DRAFT")));
        mvc.perform(get("/api/v1/teacher/courses?size=1&status=DRAFT").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].description").doesNotExist())
            .andExpect(header().string("Cache-Control","no-store"));
        verify(teacherEligibility, times(2)).requireEligible(1L);
    }
    @Test void foreignOrMissingCourseAndNonDraftAreSafe() throws Exception {
        account(Role.TEACHER,true,false);
        mvc.perform(get("/api/v1/teacher/courses/2").header("Authorization","Bearer access"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
        for (String state : List.of("PUBLISHED","COMPLETED","CANCELLED","ARCHIVED")) {
            when(courseRepository.find(1,2,true)).thenReturn(Optional.of(row(state)));
            mvc.perform(patch("/api/v1/teacher/courses/2").header("Authorization","Bearer access")
                    .contentType("application/json").content("{\"name\":\"New\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COURSE_NOT_DRAFT"));
        }
    }
    @Test void inactiveRetentionAndMergedValidation() throws Exception {
        account(Role.TEACHER,true,false);
        when(courseRepository.find(1,2,true)).thenReturn(Optional.of(row("DRAFT")));
        when(courseRepository.update(eq(1L),eq(2L),any(),eq(now))).thenReturn(row("DRAFT"));
        mvc.perform(patch("/api/v1/teacher/courses/2").header("Authorization","Bearer access")
                .contentType("application/json").content("{\"categoryId\":\"1\",\"name\":\"New\"}"))
            .andExpect(status().isOk());
        verifyNoInteractions(categoryRepository);
        mvc.perform(patch("/api/v1/teacher/courses/2").header("Authorization","Bearer access")
                .contentType("application/json").content("{\"minStudents\":151}"))
            .andExpect(status().isBadRequest());
        verify(courseRepository,times(1)).update(eq(1L),eq(2L),any(),eq(now));
    }
    @Test void categoryErrorsAndUnknownFields() throws Exception {
        account(Role.TEACHER,true,false);
        var fields = CourseInputTests.fields();
        mvc.perform(post("/api/v1/teacher/courses").header("Authorization","Bearer access")
                .contentType("application/json").content(mapper.writeValueAsString(fields)))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        when(categoryRepository.find(1,true)).thenReturn(Optional.of(new CategoryDto(new ApiId(1),"C",false,now,now)));
        mvc.perform(post("/api/v1/teacher/courses").header("Authorization","Bearer access")
                .contentType("application/json").content(mapper.writeValueAsString(fields)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATEGORY_INACTIVE"));
        fields.put("teacherId", "2");
        mvc.perform(post("/api/v1/teacher/courses").header("Authorization","Bearer access")
                .contentType("application/json").content(mapper.writeValueAsString(fields))).andExpect(status().isBadRequest());
        verifyNoInteractions(courseRepository);
    }
    @Test void authAndEligibilityForAllEndpoints() throws Exception {
        for (var request : List.of(get("/api/v1/teacher/courses"),get("/api/v1/teacher/courses/1"),
                post("/api/v1/teacher/courses"),patch("/api/v1/teacher/courses/1")))
            mvc.perform(request.cookie(new jakarta.servlet.http.Cookie("refresh_token",refresh))).andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.ADMIN,Role.STUDENT)) {
            account(role,true,false);
            mvc.perform(post("/api/v1/teacher/courses").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        account(Role.TEACHER,false,false);
        mvc.perform(get("/api/v1/teacher/courses").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        account(Role.TEACHER,true,true);
        mvc.perform(get("/api/v1/teacher/courses").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        account(Role.TEACHER,true,false);
        doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
        mvc.perform(get("/api/v1/teacher/courses").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        verifyNoInteractions(courseRepository);
    }
    @Test void queryValidationAndNoDelete() throws Exception {
        account(Role.TEACHER,true,false);
        for (String q : List.of("?page=-1","?size=101","?size=0","?page=2147483647&size=100",
                "?status=BAD","?page=0&page=1","?teacherId=2"))
            mvc.perform(get("/api/v1/teacher/courses"+q).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/teacher/courses/1").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        verifyNoInteractions(courseRepository);
    }
    @Test void databaseFailureIsSanitized() throws Exception {
        account(Role.TEACHER,true,false);
        when(courseRepository.list(1,0,20,null)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE_SQL"));
        mvc.perform(get("/api/v1/teacher/courses").header("Authorization","Bearer access"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("COURSE_UNAVAILABLE"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE_SQL"))));
    }
    @Test void scheduleDtosGenerationAndPlanningLock() throws Exception {
        account(Role.TEACHER,true,false);
        var c=row("DRAFT");
        var future=new CourseDto(c.id(),c.name(),c.status(),c.category(),c.tuition(),LocalDate.of(2030,1,1),c.timezone(),2,c.minStudents(),c.maxStudents(),now,now,c.description(),null);
        long id=c.id().value();String path="/api/v1/teacher/courses/"+id;
        when(courseRepository.find(1,id,true)).thenReturn(Optional.of(future));
        when(courseRepository.rules(id)).thenReturn(List.of(new com.englishlearning.features.course.dto.ScheduleDto.Rule(new ApiId(id),2,"19:00","20:00")));
        mvc.perform(get(path+"/schedule").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.courseId").value(Long.toString(id)))
            .andExpect(jsonPath("$.rules[0].id").value(Long.toString(id))).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(put(path+"/schedule").header("Authorization","Bearer access").contentType("application/json")
                .content("{\"rules\":[{\"dayOfWeek\":2,\"startTime\":\"19:00\",\"endTime\":\"20:00\"}]}"))
            .andExpect(status().isOk());
        verify(courseRepository,never()).replaceRules(anyLong(),any(),any());
        mvc.perform(post(path+"/sessions/generate").header("Authorization","Bearer access"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.generatedSessionCount").value(2))
            .andExpect(header().string("Location",path+"/sessions")).andExpect(header().string("Cache-Control","no-store"));
        when(courseRepository.sessionCount(id)).thenReturn(2L);
        mvc.perform(post(path+"/sessions/generate").header("Authorization","Bearer access"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSIONS_ALREADY_GENERATED"));
        mvc.perform(patch(path).header("Authorization","Bearer access").contentType("application/json").content("{\"sessionCount\":3}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COURSE_SCHEDULE_LOCKED"));
    }
    @Test void scheduleRejectsBodiesQueriesAndMalformedRules() throws Exception {
        account(Role.TEACHER,true,false);when(courseRepository.find(1,2,true)).thenReturn(Optional.of(row("DRAFT")));
        String path="/api/v1/teacher/courses/2";
        for(String body:List.of("{}","null"," ","{\"teacherId\":1}"))
            mvc.perform(post(path+"/sessions/generate").header("Authorization","Bearer access").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(post(path+"/sessions/generate?count=1").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        for(String body:List.of("{}","{\"rules\":[null]}","{\"rules\":[]}","{\"rules\":[{\"dayOfWeek\":1.0,\"startTime\":\"09:00\",\"endTime\":\"10:00\"}]}"))
            mvc.perform(put(path+"/schedule").header("Authorization","Bearer access").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verify(courseRepository,never()).insertSessions(anyLong(),any(),any());
    }
    @Test void scheduleAuthenticationEligibilityAndCors() throws Exception {
        String path="/api/v1/teacher/courses/2";
        for(var request:List.of(get(path+"/schedule"),put(path+"/schedule"),post(path+"/sessions/generate")))
            mvc.perform(request.cookie(new jakarta.servlet.http.Cookie("refresh_token",refresh))).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.STUDENT,Role.ADMIN)) {
            account(role,true,false);
            for(var request:List.of(get(path+"/schedule"),put(path+"/schedule"),post(path+"/sessions/generate")))
                mvc.perform(request.header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        account(Role.TEACHER,true,false);
        doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
        mvc.perform(get(path+"/schedule").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        mvc.perform(options(path+"/schedule").header("Origin",origin()).header("Access-Control-Request-Method","PUT")
                .header("Access-Control-Request-Headers","authorization,content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin()));
        verifyNoInteractions(courseRepository);
    }
}

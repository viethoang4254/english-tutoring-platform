package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.*;
import com.englishlearning.features.course.dto.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "spring.flyway.enabled=false","app.security.browser.local-http=false",
    "app.security.browser.allowed-origins=https://frontend.example"})
class PublicCourseHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";}
    String api(){return "https://backend.example";}
    boolean secure(){return true;}
    PublicCourseDto row(){return new PublicCourseDto(new ApiId(9007199254740993L),"English","Public description",
        new ApiMoney("250000.00","VND"),new PublicCourseDto.Category(new ApiId(2),"Speaking"),
        new PublicCourseDto.Teacher(new ApiId(3),"Teacher"),LocalDate.of(2030,1,1),"Asia/Ho_Chi_Minh",10);}
    @Test void anonymousListUsesDefaultsAndSafeMoneyAndIds() throws Exception {
        when(courseRepository.publicList(0,20)).thenReturn(List.of(row()));
        mvc.perform(get("/api/v1/public/courses")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value("9007199254740993"))
            .andExpect(jsonPath("$.items[0].tuition.amount").value("250000.00"))
            .andExpect(jsonPath("$.items[0].tuition.currency").value("VND"))
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.hasNext").value(false)).andExpect(header().string("Cache-Control","no-store"));
        verifyNoInteractions(accounts,teacherEligibility);
    }
    @Test void anonymousDetailExposesOnlyAllowlistedFieldsAndWeeklyRules() throws Exception {
        when(courseRepository.publicFind(1)).thenReturn(Optional.of(row()));
        when(courseRepository.rules(1)).thenReturn(List.of(new ScheduleDto.Rule(new ApiId(4),2,"09:00","10:00")));
        var response=mvc.perform(get("/api/v1/public/courses/1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.rules[0].dayOfWeek").value(2))
            .andExpect(jsonPath("$.course.teacher.fullName").value("Teacher"))
            .andExpect(jsonPath("$.course.timezone").value("Asia/Ho_Chi_Minh"))
            .andReturn().getResponse().getContentAsString();
        var json=mapper.readTree(response);
        var course=json.get("course");
        assertEquals(9,course.size());assertEquals(2,course.get("teacher").size());
        assertEquals(2,course.get("category").size());assertEquals(2,json.size());
        for(String field:List.of("meetUrl","password","passwordHash","email","bankAccount","content","enrollments","thumbnailPath")) {
            assertFalse(course.has(field));assertFalse(course.get("teacher").has(field));
        }
    }
    @Test void missingOrNonPublicDetailReturnsSafe404() throws Exception {
        mvc.perform(get("/api/v1/public/courses/99")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
        verify(courseRepository,never()).rules(anyLong());
    }
    @Test void paginationUsesLookaheadAndSupportsMaximumSize() throws Exception {
        when(courseRepository.publicList(2,1)).thenReturn(List.of(row(),row()));
        mvc.perform(get("/api/v1/public/courses?page=2&size=1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.page").value(2));
        mvc.perform(get("/api/v1/public/courses?size=100")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));
        verify(courseRepository).publicList(0,100);
    }
    @Test void invalidQueriesIdsAndGetBodiesAreRejected() throws Exception {
        for(String query:List.of("page=-1","size=0","size=101","page=2147483647&size=100",
                "page=x","page=0&page=1","status=DRAFT","teacherId=3"))
            mvc.perform(get("/api/v1/public/courses?"+query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/v1/public/courses/invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/public/courses/1?size=1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/public/courses").content("{}")).andExpect(status().isBadRequest());
        verifyNoInteractions(courseRepository);
    }
    @Test void publicReadPermissionDoesNotExposeManagementOrWrites() throws Exception {
        mvc.perform(get("/api/v1/teacher/courses")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/public/courses").contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/public/courses/1/sessions")).andExpect(status().isUnauthorized());
        verifyNoInteractions(courseRepository);
    }
    @Test void databaseFailureIsSanitized() throws Exception {
        when(courseRepository.publicList(0,20)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private database details"));
        var response=mvc.perform(get("/api/v1/public/courses")).andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("COURSE_UNAVAILABLE")).andReturn().getResponse().getContentAsString();
        assertFalse(response.contains("private database details"));
    }
}

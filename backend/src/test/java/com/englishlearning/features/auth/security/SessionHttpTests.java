package com.englishlearning.features.auth.security;

import com.englishlearning.common.api.*;
import com.englishlearning.features.course.dto.*;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SessionHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";}String api(){return "https://backend.example";}boolean secure(){return true;}
    static final String PATH="/api/v1/teacher/courses/42/sessions";
    void account(Role role) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        var a=new AccountSnapshot(1L,role,false,true,false);
        when(accounts.findById(1L)).thenReturn(Optional.of(a));when(credentials.lockAccountById(1L)).thenReturn(Optional.of(a));
    }
    CourseDto parent(String status) {return new CourseDto(new ApiId(42),"C",status,new CourseDto.Category(new ApiId(1),"K",true),new ApiMoney("0.00","VND"),LocalDate.of(2030,1,1),"UTC",1,1,2,now,now,"Description",null);}
    SessionDto row(String status) {return new SessionDto(new ApiId(9007199254740993L),new ApiId(42),1,null,now,now.plusSeconds(60),status,"Private text",null,now,now);}
    void setup(String course,String session) {
        account(Role.TEACHER);
        when(courseRepository.find(1,42,false)).thenReturn(Optional.of(parent(course)));
        when(courseRepository.find(1,42,true)).thenReturn(Optional.of(parent(course)));
        when(courseRepository.session(1,42,9007199254740993L,false)).thenReturn(Optional.of(row(session)));
        when(courseRepository.session(1,42,9007199254740993L,true)).thenReturn(Optional.of(row(session)));
    }
    @Test void listDefaultsPaginationFilteringAndPrivateDto() throws Exception {
        setup("DRAFT","SCHEDULED");when(courseRepository.sessions(1,42,0,20,null)).thenReturn(List.of(row("SCHEDULED")));
        mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.items[0].id").value("9007199254740993")).andExpect(jsonPath("$.items[0].courseId").value("42"))
            .andExpect(jsonPath("$.items[0].content").doesNotExist()).andExpect(jsonPath("$.items[0].createdAt").doesNotExist())
            .andExpect(header().string("Cache-Control","no-store"));
        when(courseRepository.sessions(1,42,0,1,"CANCELLED")).thenReturn(List.of(row("CANCELLED"),row("CANCELLED")));
        mvc.perform(get(PATH+"?size=1&status=CANCELLED").header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(jsonPath("$.hasNext").value(true)).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(PATH+"?size=100&page=1").header("Authorization","Bearer access")).andExpect(status().isOk());
        verify(courseRepository).sessions(1,42,1,100,null);
    }
    @Test void strictQueriesAndIds() throws Exception {
        setup("DRAFT","SCHEDULED");
        for(String q:List.of("?page=-1","?size=0","?size=101","?page=2147483647&size=100","?size=1.0","?status=BAD","?page=0&page=1","?teacherId=1","?status="))
            mvc.perform(get(PATH+q).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get(PATH+"/0").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get(PATH+"/9007199254740993?extra=1").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        verify(courseRepository,never()).sessions(anyLong(),anyLong(),anyInt(),anyInt(),any());
    }
    @Test void detailAndMissingNestedResource() throws Exception {
        setup("ARCHIVED","CANCELLED");
        mvc.perform(get(PATH+"/9007199254740993").header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("Private text")).andExpect(jsonPath("$.updatedAt").exists())
            .andExpect(jsonPath("$.meetUrl").doesNotExist()).andExpect(jsonPath("$.teacherId").doesNotExist());
        mvc.perform(get(PATH+"/2").header("Authorization","Bearer access")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
        mvc.perform(get("/api/v1/teacher/courses/99/sessions/9007199254740993").header("Authorization","Bearer access"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }
    @Test void patchNullAndOmissionWithAllowedStates() throws Exception {
        for(String state:List.of("DRAFT","PUBLISHED")) {
            setup(state,"SCHEDULED");when(courseRepository.updateSession(eq(1L),eq(42L),eq(9007199254740993L),any(),eq(now))).thenReturn(row("SCHEDULED"));
            mvc.perform(patch(PATH+"/9007199254740993").header("Authorization","Bearer access").contentType("application/json").content("{\"title\":null}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        }
        verify(courseRepository,times(2)).updateSession(1,42,9007199254740993L,new com.englishlearning.features.course.service.SessionInput(null,"Private text"),now);
    }
    @Test void invalidPatchAndLifecycleCannotWrite() throws Exception {
        setup("DRAFT","SCHEDULED");
        for(String body:List.of("{}","null","[]","{","{\"title\":\" \"}","{\"content\":42}","{\"status\":\"CANCELLED\"}","{\"scheduledStartAt\":\"2030-01-01T00:00:00Z\"}","{\"title\":\"\\uD800\"}"))
            mvc.perform(patch(PATH+"/9007199254740993").header("Authorization","Bearer access").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        for(String state:List.of("COMPLETED","CANCELLED","ARCHIVED")) {
            setup(state,"SCHEDULED");mvc.perform(patch(PATH+"/9007199254740993").header("Authorization","Bearer access").contentType("application/json").content("{\"title\":\"Text\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_NOT_EDITABLE"));
        }
        setup("PUBLISHED","CANCELLED");mvc.perform(patch(PATH+"/9007199254740993").header("Authorization","Bearer access").contentType("application/json").content("{\"title\":null}"))
            .andExpect(status().isConflict());
        verify(courseRepository,never()).updateSession(anyLong(),anyLong(),anyLong(),any(),any());
    }
    @Test void authorizationAndEligibilityOnAllRoutes() throws Exception {
        for(var r:List.of(get(PATH),get(PATH+"/1"),patch(PATH+"/1")))mvc.perform(r).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.ADMIN,Role.STUDENT)) {
            account(role);for(var r:List.of(get(PATH),get(PATH+"/1"),patch(PATH+"/1")))mvc.perform(r.header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        account(Role.TEACHER);doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
        for(var r:List.of(get(PATH),get(PATH+"/1"),patch(PATH+"/1").contentType("application/json").content("{\"title\":null}")))
            mvc.perform(r.header("Authorization","Bearer access")).andExpect(status().isForbidden());
        verifyNoInteractions(courseRepository);
    }
    void future() {
        setup("DRAFT","SCHEDULED");var r=row("SCHEDULED");
        when(courseRepository.session(1,42,r.id().value(),true)).thenReturn(Optional.of(new SessionDto(r.id(),r.courseId(),1,r.title(),
                java.time.Instant.parse("2030-01-01T12:00:00Z"),java.time.Instant.parse("2030-01-01T13:00:00Z"),r.status(),r.content(),null,now,now)));
    }
    String rescheduleBody(){return "{\"expectedStartAt\":\"2030-01-01T12:00:00Z\",\"expectedEndAt\":\"2030-01-01T13:00:00Z\",\"scheduledStartAt\":\"2030-01-02T12:00:00Z\",\"scheduledEndAt\":\"2030-01-02T13:00:00Z\"}";}
    @Test void rescheduleAndCancelSafeConflicts() throws Exception {
        future();String path=PATH+"/9007199254740993";
        when(courseRepository.reschedule(eq(1L),eq(42L),any(),any(),eq(now))).thenReturn(row("SCHEDULED"));
        mvc.perform(post(path+"/reschedule").header("Authorization","Bearer access").contentType("application/json").content(rescheduleBody()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(post(path+"/reschedule").header("Authorization","Bearer access").contentType("application/json").content(rescheduleBody().replace("2030-01-01T12","2030-01-03T12")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_CHANGED"));
        when(courseRepository.hasCalendarEvents(9007199254740993L)).thenReturn(true);
        for(String endpoint:List.of("reschedule","cancel"))mvc.perform(post(path+"/"+endpoint).header("Authorization","Bearer access").contentType("application/json").content(endpoint.equals("cancel")?"{}":rescheduleBody()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CALENDAR_SYNC_REQUIRED"));
        verify(courseRepository,never()).cancel(anyLong(),anyLong(),anyLong(),any(),any());
    }
    @Test void historyPaginationAndStrictMutations() throws Exception {
        future();String path=PATH+"/9007199254740993";
        var h=new ScheduleChangeDto(new ApiId(9007199254740994L),new ApiId(9007199254740993L),now,now.plusSeconds(60),now.plusSeconds(120),now.plusSeconds(180),now);
        when(courseRepository.history(1,42,9007199254740993L,0,1)).thenReturn(List.of(h,h));
        mvc.perform(get(path+"/schedule-changes?size=1").header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value("9007199254740994")).andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.items[0].actor").doesNotExist()).andExpect(header().string("Cache-Control","no-store"));
        for(String q:List.of("?size=101","?page=-1","?page=0&page=1","?status=SCHEDULED"))mvc.perform(get(path+"/schedule-changes"+q).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        for(String body:List.of("{}","null","{","{\"scheduledStartAt\":null}"))mvc.perform(post(path+"/reschedule").header("Authorization","Bearer access").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        for(String body:List.of("null","{\"reason\":42}","{\"reason\":\" \"}","{\"status\":\"CANCELLED\"}"))mvc.perform(post(path+"/cancel").header("Authorization","Bearer access").contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
    @Test void phaseThreeAuthorizationAndParentage() throws Exception {
        String path=PATH+"/9007199254740993";
        for(var r:List.of(get(path+"/schedule-changes"),post(path+"/reschedule"),post(path+"/cancel")))mvc.perform(r).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.STUDENT,Role.ADMIN)) {
            account(role);for(var r:List.of(get(path+"/schedule-changes"),post(path+"/reschedule"),post(path+"/cancel")))mvc.perform(r.header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        future();mvc.perform(post(PATH+"/2/cancel").header("Authorization","Bearer access").contentType("application/json").content("{}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
        doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
        for(var r:List.of(get(path+"/schedule-changes"),post(path+"/reschedule").contentType("application/json").content(rescheduleBody()),post(path+"/cancel").contentType("application/json").content("{}")))
            mvc.perform(r.header("Authorization","Bearer access")).andExpect(status().isForbidden());
    }
}

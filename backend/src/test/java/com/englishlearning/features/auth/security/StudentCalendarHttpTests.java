package com.englishlearning.features.auth.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.enrollment.dto.StudentCalendarSessionDto;
import com.englishlearning.features.enrollment.service.EnrollmentService;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;

class StudentCalendarHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://frontend.example";} String api(){return "https://backend.example";} boolean secure(){return true;}
    final String path="/api/v1/student/calendar/sessions";
    final String from="2030-01-01T00:00:00Z",to="2030-02-01T00:00:00Z";
    void account(Role role,boolean verified,boolean locked){
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,verified,role == Role.STUDENT,false)));
    }
    @Test void readOnlyDtoAndActorBoundPagination() throws Exception {
        account(Role.STUDENT,false,false);
        var item=new StudentCalendarSessionDto(new ApiId(9007199254740993L),new ApiId(42),"Course","Teacher",2,null,
                Instant.parse(from),Instant.parse(to),"Asia/Ho_Chi_Minh","CANCELLED",true);
        when(enrollmentRepository.calendar(1,Instant.parse(from),Instant.parse(to),0,1)).thenReturn(List.of(item,item));
        mvc.perform(get(path).param("from",from).param("to",to).param("size","1").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.items[0].id").value("9007199254740993"))
            .andExpect(jsonPath("$.items[0].teacherDisplayName").value("Teacher"))
            .andExpect(jsonPath("$.items[0].timezone").value("Asia/Ho_Chi_Minh"))
            .andExpect(jsonPath("$.items[0].status").value("CANCELLED"))
            .andExpect(jsonPath("$.items[0].rescheduled").value(true))
            .andExpect(jsonPath("$.items[0].meetUrl").doesNotExist())
            .andExpect(jsonPath("$.items[0].content").doesNotExist()).andExpect(jsonPath("$.items[0].payment").doesNotExist());
        verify(enrollmentRepository).calendar(1,Instant.parse(from),Instant.parse(to),0,1);
        verifyNoMoreInteractions(enrollmentRepository);
        assertTrue(EnrollmentService.class.getMethod("calendar",long.class,String.class,String.class,int.class,int.class)
                .getAnnotation(Transactional.class).readOnly());
    }
    @Test void offsetsDstAndEmptyResults() throws Exception {
        account(Role.STUDENT,true,false);
        mvc.perform(get(path).param("from","2030-03-10T01:30:00-05:00").param("to","2030-03-10T03:30:00-04:00").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.hasNext").value(false));
        verify(enrollmentRepository).calendar(1,Instant.parse("2030-03-10T06:30:00Z"),Instant.parse("2030-03-10T07:30:00Z"),0,20);
    }
    @Test void dateAndPaginationBounds() throws Exception {
        account(Role.STUDENT,true,false);
        for(String start:List.of("","2030-01-01","2030-01-01T00:00:00",to,"2031-01-01T00:00:00Z","2020-01-01T00:00:00Z","0000-01-01T00:00:00Z"))
            mvc.perform(get(path).param("from",start).param("to",to).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        for(String query:List.of("size=101","size=0","page=-1","page=2147483647&size=100","studentId=2","from=x","status=SCHEDULED"))
            mvc.perform(get(path+"?"+query).param("from",from).param("to",to).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("from",from).param("to",to).content("{}").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("from",from).param("to",Instant.parse(from).plusSeconds(62*86400+1).toString()).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        verifyNoInteractions(enrollmentRepository);
        mvc.perform(get(path).param("from",from).param("to",Instant.parse(from).plusSeconds(62*86400).toString()).param("size","100").header("Authorization","Bearer access")).andExpect(status().isOk());
    }
    @Test void authenticationRolesAndLockedAccount() throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.TEACHER,Role.ADMIN)) {
            account(role,true,false);mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        account(Role.STUDENT,true,true);mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        verifyNoInteractions(enrollmentRepository);
    }
    @Test void infrastructureErrorsDoNotLeakDetails() throws Exception {
        account(Role.STUDENT,true,false);
        when(enrollmentRepository.calendar(anyLong(),any(),any(),anyInt(),anyInt())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE"));
        mvc.perform(get(path).param("from",from).param("to",to).header("Authorization","Bearer access"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ENROLLMENT_UNAVAILABLE"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE"))));
    }
}

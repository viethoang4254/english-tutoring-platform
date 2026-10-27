package com.englishlearning.features.auth.security;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.course.dto.CalendarSessionDto;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
class TeacherCalendarHttpTests extends AuthenticationHttpTestSupport {
 String origin(){return "https://frontend.example";} String api(){return "https://backend.example";} boolean secure(){return true;}
 final String path="/api/v1/teacher/calendar/sessions";
 final String from="2030-01-01T00:00:00Z",to="2030-02-01T00:00:00Z";
 void account(Role role,boolean verified,boolean locked){
  when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
  when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,verified,false)));
 }
 @Test void safeDtoPaginationAndActorBinding() throws Exception {
  account(Role.TEACHER,true,false);
  var item=new CalendarSessionDto(new ApiId(9007199254740993L),new ApiId(42),"Course",2,null,Instant.parse(from),Instant.parse(to),"Asia/Ho_Chi_Minh","CANCELLED");
  when(courseRepository.calendar(1,Instant.parse(from),Instant.parse(to),0,1)).thenReturn(List.of(item,item));
  mvc.perform(get(path).param("from",from).param("to",to).param("size","1").header("Authorization","Bearer access"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.hasNext").value(true))
   .andExpect(jsonPath("$.items[0].sessionId").value("9007199254740993"))
   .andExpect(jsonPath("$.items[0].status").value("CANCELLED"))
   .andExpect(jsonPath("$.items[0].content").doesNotExist()).andExpect(jsonPath("$.items[0].payment").doesNotExist());
  verify(teacherEligibility).requireEligible(1L);
  verify(courseRepository).calendar(1,Instant.parse(from),Instant.parse(to),0,1);
 }
 @Test void offsetConversionAndDefaults() throws Exception {
  account(Role.TEACHER,true,false);
  mvc.perform(get(path).param("from","2030-01-01T07:00:00+07:00").param("to",to).header("Authorization","Bearer access"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.hasNext").value(false));
  verify(courseRepository).calendar(1,Instant.parse(from),Instant.parse(to),0,20);
 }
 @Test void invalidRangesQueriesAndBodies() throws Exception {
  account(Role.TEACHER,true,false);
  for(String start:List.of("","2030-01-01",to,"2031-01-01T00:00:00Z","2020-01-01T00:00:00Z"))
   mvc.perform(get(path).param("from",start).param("to",to).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
  mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
  for(String query:List.of("size=101","size=0","page=-1","page=2147483647&size=100","teacherId=2","from=x","status=SCHEDULED"))
   mvc.perform(get(path+"?"+query).param("from",from).param("to",to).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
  mvc.perform(get(path).param("from",from).param("to",to).content("{}").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
  verifyNoInteractions(courseRepository);
 }
 @Test void authenticationAndRoles() throws Exception {
  mvc.perform(get(path)).andExpect(status().isUnauthorized());
  for(Role role:List.of(Role.STUDENT,Role.ADMIN)){account(role,true,false);mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isForbidden());}
  account(Role.TEACHER,false,false);mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isForbidden());
  account(Role.TEACHER,true,true);mvc.perform(get(path).header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
  verifyNoInteractions(courseRepository);
 }
 @Test void pendingOrRejectedEligibilityDenied() throws Exception {
  account(Role.TEACHER,true,false);doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
  mvc.perform(get(path).param("from",from).param("to",to).header("Authorization","Bearer access")).andExpect(status().isForbidden());verifyNoInteractions(courseRepository);
 }
 @Test void databaseFailureIsSafe() throws Exception {
  account(Role.TEACHER,true,false);when(courseRepository.calendar(anyLong(),any(),any(),anyInt(),anyInt())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE"));
  mvc.perform(get(path).param("from",from).param("to",to).header("Authorization","Bearer access"))
   .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("COURSE_UNAVAILABLE"));
 }
}

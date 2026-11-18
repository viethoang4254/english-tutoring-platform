package com.englishlearning.features.auth.security;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.util.*;
import com.englishlearning.common.api.ApiId;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@org.springframework.boot.test.context.SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000"})
class AdminCourseHttpTests extends AuthenticationHttpTestSupport {
 String origin(){return "http://localhost:3000";}String api(){return "http://localhost:8080";}boolean secure(){return false;}
 void actor(Role role,boolean locked,boolean verified){when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,verified,true)));}
 @Test void onlyEligibleAdminCanRead()throws Exception {
  mvc.perform(get("/api/v1/admin/courses")).andExpect(status().isUnauthorized());
  for(Role role:List.of(Role.STUDENT,Role.TEACHER)){actor(role,false,true);mvc.perform(get("/api/v1/admin/courses").header("Authorization","Bearer access")).andExpect(status().isForbidden());mvc.perform(get("/api/v1/admin/courses/2").header("Authorization","Bearer access")).andExpect(status().isForbidden());}
  actor(Role.ADMIN,true,true);mvc.perform(get("/api/v1/admin/courses").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
  actor(Role.ADMIN,false,false);mvc.perform(get("/api/v1/admin/courses").header("Authorization","Bearer access")).andExpect(status().isForbidden());verifyNoInteractions(courseRepository);
 }
 @Test void detailAllowlistAndPaginationExcludeProtectedFields()throws Exception {
  actor(Role.ADMIN,false,true);
  var item=new com.englishlearning.features.course.dto.AdminCourseDto(new ApiId(2),"Course","Description",new com.englishlearning.common.api.ApiMoney("1000000.00","VND"),
   new com.englishlearning.features.course.dto.PublicCourseDto.Teacher(new ApiId(3),"Teacher"),new com.englishlearning.features.course.dto.PublicCourseDto.Category(new ApiId(4),"Category"),"DRAFT","Asia/Ho_Chi_Minh",7,4,3,3,2,null,null);
  when(courseRepository.oversight(0,1,"",null,null,null,2L)).thenReturn(List.of(item));
  mvc.perform(get("/api/v1/admin/courses/2").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
   .andExpect(jsonPath("$.enrollmentCount").value(7)).andExpect(jsonPath("$.activeEnrollmentCount").value(4))
   .andExpect(jsonPath("$.meetUrl").doesNotExist()).andExpect(jsonPath("$.students").doesNotExist()).andExpect(jsonPath("$.teacher.email").doesNotExist());
  when(courseRepository.oversight(0,1,"",null,null,null,null)).thenReturn(List.of(item,item));
  mvc.perform(get("/api/v1/admin/courses?size=1").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.hasNext").value(true));
  mvc.perform(post("/api/v1/admin/courses").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access")).andExpect(status().isForbidden());
 }
 @Test void filtersAndValidationAreReadOnly()throws Exception {
  actor(Role.ADMIN,false,true);when(courseRepository.oversight(1,20,"English",2L,3L,"DRAFT",null)).thenReturn(List.of());
  mvc.perform(get("/api/v1/admin/courses?page=1&search=English&teacherId=2&categoryId=3&status=DRAFT").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.items.length()").value(0));
  verify(courseRepository).oversight(1,20,"English",2L,3L,"DRAFT",null);
  for(String query:List.of("size=101","page=2147483647","teacherId=0","categoryId=bad","status=HIDDEN","search=x&search=y","meetUrl=x"))mvc.perform(get("/api/v1/admin/courses?"+query).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
  when(courseRepository.oversight(0,1,"",null,null,null,9L)).thenReturn(List.of());mvc.perform(get("/api/v1/admin/courses/9").header("Authorization","Bearer access")).andExpect(status().isNotFound());
  for(String path:List.of("/api/v1/admin/courses","/api/v1/teacher/courses","/api/v1/teacher/courses/2/publish","/api/v1/teacher/courses/2/sessions/generate"))mvc.perform(post(path).header("Authorization","Bearer access").contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
}

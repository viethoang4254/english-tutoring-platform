package com.englishlearning.features.auth.security;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.dto.AdminUserDto;
import com.englishlearning.common.api.ApiId;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@org.springframework.boot.test.context.SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000"})
class AdminUserHttpTests extends AuthenticationHttpTestSupport {
 String origin(){return "http://localhost:3000";} String api(){return "http://localhost:8080";} boolean secure(){return false;}
 void actor(Role role,boolean locked){when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,true,false)));}
 @Test void directoryIsPrivateAndFiltersAreForwarded() throws Exception {
  actor(Role.ADMIN,false);
  when(adminUsers.list(1,1,"Student","STUDENT",false)).thenReturn(List.of(new AdminUserDto(new ApiId(3),"Student","s@example.invalid","STUDENT",false,false),new AdminUserDto(new ApiId(4),"Other","o@example.invalid","STUDENT",true,false)));
  mvc.perform(get("/api/v1/admin/users?page=1&size=1&search=Student&role=STUDENT&locked=false").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.hasNext").value(true)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].passwordHash").doesNotExist()).andExpect(header().string("Cache-Control","no-store"));
 }
 @Test void rolesAndLockedAccountsCannotReadDirectory() throws Exception {
  for(Role role:List.of(Role.STUDENT,Role.TEACHER)){actor(role,false);mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer access")).andExpect(status().isForbidden());}
  actor(Role.ADMIN,true);mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());verifyNoInteractions(adminUsers);
 }
 @Test void lockRequiresCsrfAndRejectsSelfAndRoleEditing() throws Exception {
  actor(Role.ADMIN,false);
  mvc.perform(patch("/api/v1/admin/users/2/lock").header("Authorization","Bearer access").contentType("application/json").content("{\"locked\":true}")).andExpect(status().isForbidden());
  when(adminUsers.find(1)).thenReturn(Optional.of(new AdminUserDto(new ApiId(1),"Admin","a@example.invalid","ADMIN",true,false)));
  mvc.perform(patch("/api/v1/admin/users/1/lock").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content("{\"locked\":true}")).andExpect(status().isForbidden());
  mvc.perform(patch("/api/v1/admin/users/2/lock").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content("{\"locked\":true,\"role\":\"ADMIN\"}")).andExpect(status().isBadRequest());verify(adminUsers,never()).setLocked(anyLong(),anyBoolean());
 }
 // MockMvc csrf() replaces the shared filter repository; this test must exercise the real cookie repository.
 @org.springframework.test.annotation.DirtiesContext(methodMode=org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD)
 @Test void realAdminCsrfCookieHeaderAllowsLockAndRejectsMissingCookie() throws Exception {
  actor(Role.ADMIN,false);
  var response=mvc.perform(get("/api/v1/admin/csrf").header("Authorization","Bearer access")).andExpect(status().isOk()).andReturn().getResponse();
  var cookie=org.springframework.mock.web.MockCookie.parse(response.getHeaders("Set-Cookie").stream().filter(v->v.startsWith("ADMIN-XSRF-TOKEN=")).findFirst().orElseThrow());
  org.junit.jupiter.api.Assertions.assertTrue(cookie.isHttpOnly());org.junit.jupiter.api.Assertions.assertEquals("/api/v1/admin",cookie.getPath());
  String token=mapper.readTree(response.getContentAsString()).path("token").asText();
  when(adminUsers.find(1)).thenReturn(Optional.of(new AdminUserDto(new ApiId(1),"Admin","a@example.invalid","ADMIN",true,false)));
  when(adminUsers.find(2)).thenReturn(Optional.of(new AdminUserDto(new ApiId(2),"Student","s@example.invalid","STUDENT",false,false)),Optional.of(new AdminUserDto(new ApiId(2),"Student","s@example.invalid","STUDENT",false,true)));
  mvc.perform(patch("/api/v1/admin/users/2/lock").header("Authorization","Bearer access").header("X-XSRF-TOKEN",token).contentType("application/json").content("{\"locked\":true}")).andExpect(status().isForbidden());
  org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
  try {
  mvc.perform(patch("/api/v1/admin/users/2/lock").cookie(cookie).header("Authorization","Bearer access").header("X-XSRF-TOKEN",token).contentType("application/json").content("{\"locked\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.locked").value(true));
  } finally {org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
  verify(adminUsers).setLocked(2,true);
 }
 @Test void teacherApprovalAlsoRequiresCsrf() throws Exception {
  actor(Role.ADMIN,false);mvc.perform(post("/api/v1/admin/teacher-applications/2/approve").header("Authorization","Bearer access")).andExpect(status().isForbidden());verifyNoInteractions(teacherReviews);
 }

 @Test void creationUsesOnboardingAndNeverCreatesAdmin() throws Exception {
  actor(Role.ADMIN,false);
  String student="{\"email\":\"new@example.invalid\",\"fullName\":\"New Student\",\"role\":\"STUDENT\",\"password\":\"safe-fixture-password\",\"confirmPassword\":\"safe-fixture-password\",\"username\":\"Student_new\",\"phone\":\"0912345678\"}";
  mvc.perform(post("/api/v1/admin/users").header("Authorization","Bearer access").contentType("application/json").content(student)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/admin/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content(student)).andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
  verify(registration).register(1L,"new@example.invalid","safe-fixture-password","New Student","student_new","+84912345678");
  String teacher="{\"email\":\"teacher@example.invalid\",\"fullName\":\"Teacher\",\"role\":\"TEACHER\",\"password\":\"safe-fixture-password\",\"confirmPassword\":\"safe-fixture-password\",\"specialization\":\"English\",\"teachingExperience\":\"Experience\",\"introduction\":\"Introduction\"}";
  mvc.perform(post("/api/v1/admin/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content(teacher)).andExpect(status().isCreated());
  verify(registration).registerTeacher(1L,"teacher@example.invalid","safe-fixture-password","Teacher","English","Experience","Introduction");
  mvc.perform(post("/api/v1/admin/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content(student.replace("STUDENT","ADMIN"))).andExpect(status().isBadRequest());
  doThrow(new com.englishlearning.features.auth.service.VerificationFailure(com.englishlearning.features.auth.service.VerificationFailure.Reason.CONFLICT)).when(registration).register(eq(1L),anyString(),anyString(),anyString(),anyString(),anyString());
  mvc.perform(post("/api/v1/admin/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content(student)).andExpect(status().isConflict());
 }
 @Test void deleteAndProfileRequireCsrfAndPreserveDependencies() throws Exception {
  actor(Role.ADMIN,false);
  when(adminUsers.find(1)).thenReturn(Optional.of(new AdminUserDto(new ApiId(1),"Admin","a@example.invalid","ADMIN",true,false)));
  when(adminUsers.find(2)).thenReturn(Optional.of(new AdminUserDto(new ApiId(2),"Student","s@example.invalid","STUDENT",false,false)));
  mvc.perform(delete("/api/v1/admin/users/2").header("Authorization","Bearer access")).andExpect(status().isForbidden());
  mvc.perform(delete("/api/v1/admin/users/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access")).andExpect(status().isForbidden());
  when(adminUsers.hasBusinessDependencies(2)).thenReturn(true);
  mvc.perform(delete("/api/v1/admin/users/2").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USER_DEPENDENCIES_EXIST"));
  verify(adminUsers,never()).deleteDisposable(anyLong());
  mvc.perform(patch("/api/v1/admin/users/2/profile").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content("{\"role\":\"ADMIN\"}")).andExpect(status().isBadRequest());
  when(adminUsers.hasBusinessDependencies(2)).thenReturn(false);
  org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
  try {mvc.perform(delete("/api/v1/admin/users/2").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access")).andExpect(status().isNoContent());}
  finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
  verify(adminUsers).deleteDisposable(2);
 }

 @Test void studentTeacherAndLockedAdminCannotMutateManagedUsers() throws Exception {
  for(Role role:List.of(Role.STUDENT,Role.TEACHER,Role.ADMIN)){
   actor(role,role==Role.ADMIN);
   when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,role==Role.ADMIN,true,true)));
   var denied=role==Role.ADMIN?status().isUnauthorized():status().isForbidden();
   mvc.perform(delete("/api/v1/admin/users/2").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access")).andExpect(denied);
   mvc.perform(patch("/api/v1/admin/users/2/profile").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content("{\"fullName\":\"Changed\"}")).andExpect(denied);
   mvc.perform(post("/api/v1/admin/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access").contentType("application/json").content("{}")).andExpect(denied);
  }
  verifyNoInteractions(adminUsers,registration);
 }
}

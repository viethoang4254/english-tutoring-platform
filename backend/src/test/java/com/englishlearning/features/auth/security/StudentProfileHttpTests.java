package com.englishlearning.features.auth.security;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.dto.StudentProfile;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.common.api.ApiId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000"})
class StudentProfileHttpTests extends AuthenticationHttpTestSupport {
 String origin(){return "http://localhost:3000";}String api(){return "http://localhost:8080";}boolean secure(){return false;}
 void account(Role role,boolean locked){when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,false,role==Role.STUDENT)));}
 @Test void safeProjectionAndBearerOnlyPatch()throws Exception{
  account(Role.STUDENT,false);var dto=new StudentProfile(new ApiId(9007199254740993L),"Name",null,"a@example.invalid",null,false);when(studentProfiles.read(1L)).thenReturn(dto);when(studentProfiles.update(eq(1L),any())).thenReturn(dto);
  var result=mvc.perform(get("/api/v1/users/me/profile").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.id").value("9007199254740993")).andReturn();assertEquals(6,mapper.readTree(result.getResponse().getContentAsString()).size());
  mvc.perform(patch("/api/v1/users/me/profile").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"New\"}")).andExpect(status().isOk());verify(studentProfiles).update(1L,java.util.Map.of("fullName","New"));
  mvc.perform(options("/api/v1/users/me/profile").header("Origin",origin()).header("Access-Control-Request-Method","PATCH").header("Access-Control-Request-Headers","Authorization,Content-Type")).andExpect(status().isOk());
 }
 @Test void deniesWrongRolesLockedAndCookieOnly()throws Exception{
  account(Role.ADMIN,false);mvc.perform(get("/api/v1/users/me/profile").header("Authorization","Bearer access")).andExpect(status().isForbidden());
  account(Role.STUDENT,true);mvc.perform(get("/api/v1/users/me/profile").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/users/me/profile").cookie(new jakarta.servlet.http.Cookie("refresh_token",refresh))).andExpect(status().isUnauthorized());verifyNoInteractions(studentProfiles);
 }
 @Test void teacherCanUseOnlyOwnPersonalProfileRoutes()throws Exception{
  account(Role.TEACHER,false);when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true,false)));
  var dto=new StudentProfile(new ApiId(1L),"Teacher",null,"teacher@example.invalid",null,true);
  when(studentProfiles.read(1L)).thenReturn(dto);when(studentProfiles.update(eq(1L),any())).thenReturn(dto);
  mvc.perform(get("/api/v1/users/me/profile").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.emailVerified").value(true));
  mvc.perform(patch("/api/v1/users/me/profile").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"0912345678\"}")).andExpect(status().isOk());
  verify(studentProfiles).update(1L,java.util.Map.of("phone","0912345678"));
  mvc.perform(get("/api/v1/users/me/profile?userId=2").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/users/2/profile").header("Authorization","Bearer access")).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/teacher/courses").header("Authorization","Bearer access")).andExpect(status().isForbidden());
  account(Role.TEACHER,true);mvc.perform(patch("/api/v1/users/me/profile").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Denied\"}")).andExpect(status().isUnauthorized());
 }
 @Test void rejectsQueryAndPreservesSafeFailures()throws Exception{
  account(Role.STUDENT,false);mvc.perform(get("/api/v1/users/me/profile?userId=2").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
  when(studentProfiles.read(1L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE"));mvc.perform(get("/api/v1/users/me/profile").header("Authorization","Bearer access")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PROFILE_UNAVAILABLE"));
 }
}

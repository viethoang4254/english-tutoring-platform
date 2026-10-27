package com.englishlearning.features.auth.security;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.category.service.*;
import com.englishlearning.features.category.dto.CategoryDto;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.common.api.ApiId;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000"})
class CategoryHttpTests extends AuthenticationHttpTestSupport {
 @MockitoBean CategoryService categories;
 String origin(){return "http://localhost:3000";}String api(){return "http://localhost:8080";}boolean secure(){return false;}
 void account(Role role,boolean verified,boolean locked){when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,verified,false)));}
 @Test void listAndMutationsUseSafeDto()throws Exception{
 account(Role.ADMIN,true,false);var dto=new CategoryDto(new ApiId(9007199254740993L),"English",true,now,now);when(categories.list(1L,0,20,null)).thenReturn(new CategoryService.Page(List.of(dto),0,20,false));when(categories.create(eq(1L),any())).thenReturn(dto);when(categories.update(eq(1L),eq(2L),any())).thenReturn(dto);
 mvc.perform(get("/api/v1/admin/categories").header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.items[0].id").value("9007199254740993"));
 mvc.perform(post("/api/v1/admin/categories").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"English\"}")).andExpect(status().isCreated());
 mvc.perform(patch("/api/v1/admin/categories/2").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());
 }
 @Test void strictQueryAndIdentifiers()throws Exception{account(Role.ADMIN,true,false);for(String q:List.of("?page=-1","?size=abc","?page=0&page=1","?active=TRUE","?other=1"))mvc.perform(get("/api/v1/admin/categories"+q).header("Authorization","Bearer access")).andExpect(status().isBadRequest());for(String id:List.of("0","-1","abc","9223372036854775808"))mvc.perform(patch("/api/v1/admin/categories/"+id).header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isBadRequest());verifyNoInteractions(categories);}
 @Test void roleAndTransportBoundaries()throws Exception{
 mvc.perform(get("/api/v1/admin/categories")).andExpect(status().isUnauthorized());
 for(Role role:List.of(Role.STUDENT,Role.TEACHER)){account(role,true,false);mvc.perform(post("/api/v1/admin/categories").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"English\"}")).andExpect(status().isForbidden());}
 account(Role.ADMIN,false,false);mvc.perform(get("/api/v1/admin/categories").header("Authorization","Bearer access")).andExpect(status().isForbidden());account(Role.ADMIN,true,true);mvc.perform(get("/api/v1/admin/categories").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
 account(Role.ADMIN,true,false);mvc.perform(delete("/api/v1/admin/categories/1").header("Authorization","Bearer access")).andExpect(status().isForbidden());mvc.perform(post("/api/v1/admin/courses").header("Authorization","Bearer access")).andExpect(status().isForbidden());mvc.perform(get("/api/v1/admin/categories").cookie(new jakarta.servlet.http.Cookie("refresh_token",refresh))).andExpect(status().isUnauthorized());verifyNoInteractions(categories);
 }
 @Test void safeErrors()throws Exception{account(Role.ADMIN,true,false);when(categories.update(eq(1L),eq(1L),any())).thenThrow(new CategoryFailure(CategoryFailure.Reason.MISSING));mvc.perform(patch("/api/v1/admin/categories/1").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));}
 @Test void duplicateConstraintMapsTo409WithoutSqlDisclosure()throws Exception{
 account(Role.ADMIN,true,false);
 var sql=new org.hibernate.exception.ConstraintViolationException("PRIVATE",new java.sql.SQLException("PRIVATE","23505"),"uq_course_categories_normalized_name");
 when(categories.create(eq(1L),any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("PRIVATE",sql));
 mvc.perform(post("/api/v1/admin/categories").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Duplicate\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATEGORY_NAME_CONFLICT")).andExpect(header().string("Cache-Control","no-store")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE"))));
 }
}

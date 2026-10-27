package com.englishlearning.features.auth.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.category.dto.CategoryDto;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TeacherCategoryHttpTests extends AuthenticationHttpTestSupport {
    String origin() { return "https://frontend.example"; }
    String api() { return "https://backend.example"; }
    boolean secure() { return true; }
    void account(Role role, boolean verified, boolean locked) {
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L, role, locked, verified, false)));
    }
    @Test void activeOnlyPaginationAndStringIds() throws Exception {
        account(Role.TEACHER,true,false);
        var item = new CategoryDto(new ApiId(9007199254740993L),"English",true,now,now);
        when(categoryRepository.list(0,1,true)).thenReturn(List.of(item,item));
        mvc.perform(get("/api/v1/teacher/categories?size=1").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value("9007199254740993"))
            .andExpect(jsonPath("$.items[0].active").value(true))
            .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.hasNext").value(true));
        verify(teacherEligibility).requireEligible(1L);
        verify(categoryRepository).list(0,1,true);
        verifyNoMoreInteractions(categoryRepository);
    }
    @Test void defaultAndEmptyPage() throws Exception {
        account(Role.TEACHER,true,false);
        when(categoryRepository.list(0,20,true)).thenReturn(List.of());
        mvc.perform(get("/api/v1/teacher/categories").header("Authorization","Bearer access"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.hasNext").value(false));
    }
    @Test void rejectsFilterOverrideAndInvalidPagination() throws Exception {
        account(Role.TEACHER,true,false);
        for (String query : List.of("?active=false","?active=true","?page=-1","?size=0","?size=101",
                "?page=2147483647&size=100","?size=x","?page=0&page=1","?other=1"))
            mvc.perform(get("/api/v1/teacher/categories"+query).header("Authorization","Bearer access"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/v1/teacher/categories").header("Authorization","Bearer access").content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(categoryRepository);
    }
    @Test void roleAccountAndCookieBoundaries() throws Exception {
        mvc.perform(get("/api/v1/teacher/categories")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/teacher/categories").cookie(new jakarta.servlet.http.Cookie("refresh_token",refresh)))
            .andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.STUDENT,Role.ADMIN)) {
            account(role,true,false);
            mvc.perform(get("/api/v1/teacher/categories").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        account(Role.TEACHER,false,false);
        mvc.perform(get("/api/v1/teacher/categories").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        account(Role.TEACHER,true,true);
        mvc.perform(get("/api/v1/teacher/categories").header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        verifyNoInteractions(categoryRepository);
    }
    @Test void onboardingDenialIsSafeAndDoesNotQueryCategories() throws Exception {
        account(Role.TEACHER,true,false);
        doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teacherEligibility).requireEligible(1L);
        mvc.perform(get("/api/v1/teacher/categories").header("Authorization","Bearer access"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(categoryRepository);
    }
    @Test void teacherCannotMutateEitherCategoryRoute() throws Exception {
        account(Role.TEACHER,true,false);
        for (String path : List.of("/api/v1/teacher/categories","/api/v1/admin/categories")) {
            mvc.perform(post(path).header("Authorization","Bearer access")).andExpect(status().isForbidden());
            mvc.perform(patch(path+"/1").header("Authorization","Bearer access")).andExpect(status().isForbidden());
            mvc.perform(delete(path+"/1").header("Authorization","Bearer access")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(categoryRepository);
    }
    @Test void databaseFailureIsSanitized() throws Exception {
        account(Role.TEACHER,true,false);
        when(categoryRepository.list(0,20,true)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE_SQL"));
        mvc.perform(get("/api/v1/teacher/categories").header("Authorization","Bearer access"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CATEGORY_UNAVAILABLE"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE_SQL"))));
    }
}

package com.englishlearning.features.category.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.category.repository.CategoryRepository;
import com.englishlearning.features.teacher.repository.TeacherApplicationRepository;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.junit.jupiter.api.Test;

class TeacherCategoryServiceTests {
    @Test void realEligibilityRejectsUnapprovedTeachersBeforeReadingCategories() {
        var accounts = mock(CurrentAccountService.class);
        var applications = mock(TeacherApplicationRepository.class);
        var categories = mock(CategoryRepository.class);
        var service = new TeacherCategoryService(categories, new TeacherBusinessEligibility(accounts, applications));
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true,false)));
        when(applications.existsByTeacherIdAndStatus(1L,"APPROVED")).thenReturn(false,true,false);
        assertThrows(CategoryFailure.class, () -> service.list(1L,0,20));
        verifyNoInteractions(categories);
        when(categories.list(0,20,true)).thenReturn(List.of());
        assertTrue(service.list(1L,0,20).items().isEmpty());
        assertThrows(CategoryFailure.class, () -> service.list(1L,0,20));
        verify(categories, times(1)).list(0,20,true);
    }

    @Test void repositoryFiltersBeforePagination() {
        var em = mock(jakarta.persistence.EntityManager.class);
        var query = mock(jakarta.persistence.Query.class);
        when(em.createNativeQuery("SELECT id,name,active,created_at,updated_at FROM public.course_categories WHERE active=:active ORDER BY id ASC")).thenReturn(query);
        when(query.setParameter("active",true)).thenReturn(query);
        when(query.setFirstResult(20)).thenReturn(query);
        when(query.setMaxResults(21)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        assertTrue(new CategoryRepository(em).list(1,20,true).isEmpty());
        verify(query).setParameter("active",true);
        verify(query).setFirstResult(20);
        verify(query).setMaxResults(21);
    }
}

package com.englishlearning.features.teacher.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.teacher.repository.TeacherApplicationRepository;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.junit.jupiter.api.Test;

class TeacherBusinessEligibilityTests {
    final CurrentAccountService accounts=mock(CurrentAccountService.class);
    final TeacherApplicationRepository applications=mock(TeacherApplicationRepository.class);
    final TeacherBusinessEligibility policy=new TeacherBusinessEligibility(accounts,applications);
    @Test void currentAccountMustBeTeacherVerifiedUnlockedAndPresent() {
        for(var state:List.of(Optional.<AccountSnapshot>empty(),
                Optional.of(new AccountSnapshot(1L,Role.STUDENT,false,true)),
                Optional.of(new AccountSnapshot(1L,Role.ADMIN,false,true)),
                Optional.of(new AccountSnapshot(1L,Role.TEACHER,true,true)),
                Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,false)))) {
            when(accounts.findById(1L)).thenReturn(state);
            assertEquals(TeacherReviewFailure.Reason.DENIED,assertThrows(TeacherReviewFailure.class,
                    ()->policy.requireEligible(1L)).reason());
        }
        verifyNoInteractions(applications);
    }
    @Test void approvalIsDatabaseBackedAndRecheckedOnEachInvocation() {
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true)));
        when(applications.existsByTeacherIdAndStatus(1L,"APPROVED")).thenReturn(false,true,false);
        assertThrows(TeacherReviewFailure.class,()->policy.requireEligible(1L));
        policy.requireEligible(1L);
        assertThrows(TeacherReviewFailure.class,()->policy.requireEligible(1L));
        verify(accounts,times(3)).findById(1L);verify(applications,times(3)).existsByTeacherIdAndStatus(1L,"APPROVED");
    }
    @Test void databaseFailureNeverGrantsAccess() {
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true)));
        when(applications.existsByTeacherIdAndStatus(1L,"APPROVED"))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("private"));
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,()->policy.requireEligible(1L));
    }
}

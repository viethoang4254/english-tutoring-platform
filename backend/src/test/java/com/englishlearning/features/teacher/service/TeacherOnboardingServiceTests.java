package com.englishlearning.features.teacher.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.teacher.repository.TeacherApplicationRepository;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.util.*;
import org.junit.jupiter.api.Test;

class TeacherOnboardingServiceTests {
    final CurrentAccountService accounts=mock(CurrentAccountService.class);
    final TeacherApplicationRepository applications=mock(TeacherApplicationRepository.class);
    final TeacherOnboardingService service=new TeacherOnboardingService(accounts,applications);
    @Test void rejectsIneligibleCurrentAccountsBeforeApplicationAccess() {
        for(var state:List.of(Optional.<AccountSnapshot>empty(),Optional.of(new AccountSnapshot(1L,Role.STUDENT,false,true)),
                Optional.of(new AccountSnapshot(1L,Role.ADMIN,false,true)),Optional.of(new AccountSnapshot(1L,Role.TEACHER,true,true)),
                Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,false)))) {
            when(accounts.findById(1L)).thenReturn(state);
            assertEquals(TeacherReviewFailure.Reason.DENIED,assertThrows(TeacherReviewFailure.class,()->service.ownStatus(1L)).reason());
        }
        verifyNoInteractions(applications);
    }
    @Test void missingApplicationDoesNotFallBackToAnotherTeacherOrRequireApproval() {
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true)));
        when(applications.findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(1L)).thenReturn(Optional.empty());
        assertEquals(TeacherReviewFailure.Reason.ONBOARDING_NOT_FOUND,assertThrows(TeacherReviewFailure.class,()->service.ownStatus(1L)).reason());
        verify(applications).findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(1L);verifyNoMoreInteractions(applications);
    }
}

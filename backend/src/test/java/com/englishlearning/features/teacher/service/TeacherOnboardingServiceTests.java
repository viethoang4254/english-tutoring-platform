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
    final UserCredentialService users=mock(UserCredentialService.class);
    final com.englishlearning.features.teacher.repository.TeacherProfileRepository profiles=mock(com.englishlearning.features.teacher.repository.TeacherProfileRepository.class);
    final TeacherOnboardingService service=new TeacherOnboardingService(accounts,applications,
            users, profiles, java.time.Clock.fixed(java.time.Instant.EPOCH,java.time.ZoneOffset.UTC));
    @Test void resubmitUsesLockedCurrentAccountAndRejectsInvalidText() {
        when(users.lockAccountById(1L)).thenReturn(Optional.empty());
        assertEquals(TeacherReviewFailure.Reason.DENIED,assertThrows(TeacherReviewFailure.class,
                ()->service.resubmit(1L,2L,"a","b","c")).reason());
        when(users.lockAccountById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true, false)));
        for(String value:new String[]{null,""," ","a\u0000b"})
            assertEquals(TeacherReviewFailure.Reason.INVALID_REQUEST,assertThrows(TeacherReviewFailure.class,
                    ()->service.resubmit(1L,2L,value,"b","c")).reason());
        verifyNoInteractions(applications,profiles);
    }
    @Test void priorApprovalBlocksEvenWhenLatestTargetIsRejected() {
        var row=mock(com.englishlearning.features.teacher.entity.TeacherApplication.class);
        when(row.getId()).thenReturn(2L);when(row.getTeacherId()).thenReturn(1L);when(row.getStatus()).thenReturn("REJECTED");
        when(users.lockAccountById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true, false)));
        when(applications.findById(2L)).thenReturn(Optional.of(row));
        when(applications.findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(1L)).thenReturn(Optional.of(row));
        when(applications.existsByTeacherIdAndStatus(1L,"APPROVED")).thenReturn(true);
        assertEquals(TeacherReviewFailure.Reason.RESUBMISSION_NOT_ALLOWED,assertThrows(TeacherReviewFailure.class,
                ()->service.resubmit(1L,2L,"a","b","c")).reason());
        verify(applications,never()).saveAndFlush(any());
    }
    @Test void clockRollbackCannotMakeNewApplicationOlderThanTarget() {
        var row=mock(com.englishlearning.features.teacher.entity.TeacherApplication.class);
        when(row.getId()).thenReturn(2L);when(row.getTeacherId()).thenReturn(1L);when(row.getStatus()).thenReturn("REJECTED");
        var future=java.time.Instant.EPOCH.plusSeconds(10);when(row.getSubmittedAt()).thenReturn(future);
        when(users.lockAccountById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true, false)));
        when(applications.findById(2L)).thenReturn(Optional.of(row));
        when(applications.findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(1L)).thenReturn(Optional.of(row));
        when(applications.saveAndFlush(any())).thenAnswer(invocation->{
            com.englishlearning.features.teacher.entity.TeacherApplication saved=invocation.getArgument(0);
            assertEquals(future,saved.getSubmittedAt());assertEquals(" a ",saved.getSpecialization());
            var result=mock(com.englishlearning.features.teacher.entity.TeacherApplication.class);
            when(result.getId()).thenReturn(3L);when(result.getStatus()).thenReturn("PENDING");when(result.getSubmittedAt()).thenReturn(future);
            return result;
        });
        assertEquals(future,service.resubmit(1L,2L," a ","b","c").submittedAt());
    }
    @Test void rejectsIneligibleCurrentAccountsBeforeApplicationAccess() {
        for(var state:List.of(Optional.<AccountSnapshot>empty(),Optional.of(new AccountSnapshot(1L,Role.STUDENT,false,true, false)),
                Optional.of(new AccountSnapshot(1L,Role.ADMIN,false,true, false)),Optional.of(new AccountSnapshot(1L,Role.TEACHER,true,true, false)),
                Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,false, false)))) {
            when(accounts.findById(1L)).thenReturn(state);
            assertEquals(TeacherReviewFailure.Reason.DENIED,assertThrows(TeacherReviewFailure.class,()->service.ownStatus(1L)).reason());
        }
        verifyNoInteractions(applications);
    }
    @Test void missingApplicationDoesNotFallBackToAnotherTeacherOrRequireApproval() {
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true, false)));
        when(applications.findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(1L)).thenReturn(Optional.empty());
        assertEquals(TeacherReviewFailure.Reason.ONBOARDING_NOT_FOUND,assertThrows(TeacherReviewFailure.class,()->service.ownStatus(1L)).reason());
        verify(applications).findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(1L);verifyNoMoreInteractions(applications);
    }
}

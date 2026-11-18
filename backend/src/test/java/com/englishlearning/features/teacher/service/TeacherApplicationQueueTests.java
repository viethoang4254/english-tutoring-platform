package com.englishlearning.features.teacher.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.teacher.repository.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TeacherApplicationQueueTests {
    final TeacherApplicationRepository applications=mock(TeacherApplicationRepository.class);
    final CurrentAccountService accounts=mock(CurrentAccountService.class);
    final TeacherApplicationReviewService service=new TeacherApplicationReviewService(applications,
            mock(TeacherProfileRepository.class),mock(UserCredentialService.class),accounts,Clock.systemUTC());
    void admin() { when(accounts.findById(12L)).thenReturn(Optional.of(new AccountSnapshot(12L,Role.ADMIN,false,true, false))); }
    @Test void fetchesOneExtraWithoutCountsOrApplicantLookups() {
        admin();
        var row=mock(TeacherApplicationRepository.PendingRow.class);
        when(row.getId()).thenReturn(1L);when(row.getTeacherId()).thenReturn(2L);
        when(row.getStatus()).thenReturn("PENDING");when(row.getSubmittedAt()).thenReturn(Instant.EPOCH);
        when(applications.findPending(2,3)).thenReturn(List.of(row,row,row));
        var result=service.pending(12L,1,2);
        assertEquals(2,result.items().size());assertTrue(result.hasNext());
        assertEquals(1,result.page());assertEquals(2,result.size());
        verify(applications).findPending(2,3);verifyNoMoreInteractions(applications);
        verify(accounts).findById(12L);verifyNoMoreInteractions(accounts);
    }
    @Test void emptyResultsHaveNoNextPage() {
        admin();when(applications.findPending(0,21)).thenReturn(List.of());
        var result=service.pending(12L,0,20);
        assertTrue(result.items().isEmpty());assertFalse(result.hasNext());
    }
    @Test void invalidBoundsNeverQueryApplications() {
        admin();
        for(int[] input:new int[][]{{-1,20},{0,0},{0,101},{Integer.MAX_VALUE,100}})
            assertEquals(TeacherReviewFailure.Reason.INVALID_REQUEST,
                    assertThrows(TeacherReviewFailure.class,()->service.pending(12L,input[0],input[1])).reason());
        verifyNoInteractions(applications);
    }
    @Test void currentReviewerRestrictionsApplyBeforeQuery() {
        for(var state:List.of(Optional.<AccountSnapshot>empty(),Optional.of(new AccountSnapshot(12L,Role.STUDENT,false,true, false)),
                Optional.of(new AccountSnapshot(12L,Role.TEACHER,false,true, false)),Optional.of(new AccountSnapshot(12L,Role.ADMIN,true,true, false)),
                Optional.of(new AccountSnapshot(12L,Role.ADMIN,false,false, false)))) {
            when(accounts.findById(12L)).thenReturn(state);
            assertEquals(TeacherReviewFailure.Reason.DENIED,
                    assertThrows(TeacherReviewFailure.class,()->service.pending(12L,0,20)).reason());
        }
        verifyNoInteractions(applications);
    }
    @Test void listsEveryStatusWithStableBoundedPagination() {
        admin();
        for(String status:List.of("PENDING","APPROVED","REJECTED")) {
            var row=mock(TeacherApplicationRepository.PendingRow.class);
            when(row.getId()).thenReturn(1L);when(row.getTeacherId()).thenReturn(2L);when(row.getStatus()).thenReturn(status);
            when(applications.findStatus(status,2,3)).thenReturn(List.of(row,row,row));
            var result=service.list(12L,1,2,status);assertTrue(result.hasNext());assertEquals(2,result.items().size());assertEquals(status,result.items().get(0).status());
        }
        assertThrows(TeacherReviewFailure.class,()->service.list(12L,0,20,"UNKNOWN"));
    }
    @Test void searchIsNormalizedValidatedAndRoleProtected() {
        admin();when(applications.searchStatus("PENDING","teacher",0,21)).thenReturn(List.of());
        assertTrue(service.search(12L,0,20,"PENDING"," Teacher ").items().isEmpty());verify(applications).searchStatus("PENDING","teacher",0,21);
        assertThrows(TeacherReviewFailure.class,()->service.search(12L,0,20,"PENDING","x".repeat(101)));
        when(accounts.findById(12L)).thenReturn(Optional.of(new AccountSnapshot(12L,Role.TEACHER,false,true,true)));
        assertEquals(TeacherReviewFailure.Reason.DENIED,assertThrows(TeacherReviewFailure.class,()->service.search(12L,0,20,"PENDING","")).reason());
    }
}

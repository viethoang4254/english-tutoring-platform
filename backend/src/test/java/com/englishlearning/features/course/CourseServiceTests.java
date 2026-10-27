package com.englishlearning.features.course;

import com.englishlearning.features.course.service.*;
import com.englishlearning.features.course.repository.CourseRepository;
import com.englishlearning.features.category.repository.CategoryRepository;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseServiceTests {
    final CourseRepository repository=mock(CourseRepository.class);
    final CategoryRepository categories=mock(CategoryRepository.class);
    final TeacherBusinessEligibility eligibility=mock(TeacherBusinessEligibility.class);
    final UserCredentialService users=mock(UserCredentialService.class);
    final CourseService service=new CourseService(repository,categories,eligibility,users,Clock.systemUTC());
    @Test void lockedSnapshotCannotBeBypassedByCachedEligibility() {
        for(var account:List.of(new AccountSnapshot(1L,Role.TEACHER,true,true,false),
                new AccountSnapshot(1L,Role.TEACHER,false,false,false),new AccountSnapshot(1L,Role.ADMIN,false,true,false))) {
            when(users.lockAccountById(1L)).thenReturn(Optional.of(account));
            assertEquals(CourseFailure.Reason.DENIED,assertThrows(CourseFailure.class,()->service.create(1,CourseInputTests.fields())).reason());
        }
        verifyNoInteractions(repository,categories,eligibility);
    }
    @Test void missingAccountAndOnboardingDeniedDoNotWrite() {
        assertThrows(CourseFailure.class,()->service.create(1,CourseInputTests.fields()));
        when(users.lockAccountById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,Role.TEACHER,false,true,false)));
        doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(eligibility).requireEligible(1L);
        assertThrows(CourseFailure.class,()->service.create(1,CourseInputTests.fields()));
        verifyNoInteractions(repository,categories);
    }
    @Test void invalidPaginationDoesNotQueryCourses() {
        assertThrows(CourseFailure.class,()->service.list(1,Integer.MAX_VALUE,100,null));
        assertThrows(CourseFailure.class,()->service.list(1,0,0,null));
        assertThrows(CourseFailure.class,()->service.list(1,0,20,"INVALID"));
        verifyNoInteractions(repository);
    }
}

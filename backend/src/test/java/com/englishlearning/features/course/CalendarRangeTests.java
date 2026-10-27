package com.englishlearning.features.course;
import com.englishlearning.features.course.service.*;
import com.englishlearning.features.course.repository.CourseRepository;
import com.englishlearning.features.category.repository.CategoryRepository;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;
import com.englishlearning.features.user.service.UserCredentialService;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CalendarRangeTests {
 final CourseRepository repository=mock(CourseRepository.class);
 final CourseService service=new CourseService(repository,mock(CategoryRepository.class),mock(TeacherBusinessEligibility.class),mock(UserCredentialService.class),Clock.systemUTC());
 @Test void exactOperationalBoundaryAndMaximumPageSize(){
  service.calendar(2,"2030-01-01T00:00:00Z","2030-03-04T00:00:00Z",0,100);
  verify(repository).calendar(2,Instant.parse("2030-01-01T00:00:00Z"),Instant.parse("2030-03-04T00:00:00Z"),0,100);
 }
 @Test void rejectsEqualReversedOversizedAndOutOfStorageBounds(){
  for(String end:new String[]{"2030-01-01T00:00:00Z","2029-12-31T23:59:59Z","2030-03-04T00:00:00.001Z","+10000-01-01T00:00:00Z"})
   assertThrows(CourseFailure.class,()->service.calendar(1,"2030-01-01T00:00:00Z",end,0,20));
  verifyNoInteractions(repository);
 }
 @Test void daylightSavingOffsetsRepresentAbsoluteRange(){
  service.calendar(3,"2030-03-30T00:00:00+01:00","2030-04-01T00:00:00+02:00",1,20);
  verify(repository).calendar(3,Instant.parse("2030-03-29T23:00:00Z"),Instant.parse("2030-03-31T22:00:00Z"),1,20);
 }
}

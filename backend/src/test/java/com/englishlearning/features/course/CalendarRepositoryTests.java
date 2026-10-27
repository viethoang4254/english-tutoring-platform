package com.englishlearning.features.course;
import com.englishlearning.features.course.repository.CourseRepository;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CalendarRepositoryTests {
 @Test void ownershipOverlapStableOrderAndCancelledMapping(){
  var em=mock(EntityManager.class);var query=mock(Query.class,RETURNS_SELF);when(em.createNativeQuery(anyString())).thenReturn(query);
  var from=Instant.parse("2030-01-01T00:00:00Z");var to=from.plusSeconds(3600);
  when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{9L,2L,"Course",1,null,from,to,"Europe/Paris","CANCELLED"}));
  var result=new CourseRepository(em).calendar(7,from,to,2,20);
  var sql=ArgumentCaptor.forClass(String.class);verify(em).createNativeQuery(sql.capture());
  assertTrue(sql.getValue().contains("c.teacher_id=:actor"));assertTrue(sql.getValue().contains("s.scheduled_start_at<:to AND s.scheduled_end_at>:from"));
  assertTrue(sql.getValue().contains("ORDER BY s.scheduled_start_at,s.id"));assertFalse(sql.getValue().contains("status="));
  verify(query).setParameter("actor",7L);verify(query).setParameter("from",from);verify(query).setParameter("to",to);verify(query).setFirstResult(40);verify(query).setMaxResults(21);
  assertEquals("CANCELLED",result.get(0).status());assertEquals("Europe/Paris",result.get(0).courseTimezone());
 }
}

package com.englishlearning.features.course;

import com.englishlearning.features.course.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchedulePlanTests {
    static Map<String,Object> rule(int day,String start,String end) { return Map.of("dayOfWeek",day,"startTime",start,"endTime",end); }
    static List<SchedulePlan.Rule> rules(Object... rows) { return SchedulePlan.parse(Map.of("rules",Arrays.asList(rows))); }
    static final Instant BEFORE=Instant.parse("2020-01-01T00:00:00Z");
    @Test void earliestDateStreamMergeAndAdjacentIntervals() {
        var result=SchedulePlan.generate(rules(rule(2,"19:00","20:00"),rule(2,"20:00","21:00"),rule(5,"19:00","20:00")),LocalDate.of(2028,2,28),"Asia/Ho_Chi_Minh",7,BEFORE);
        assertEquals(7,result.size()); assertEquals(Instant.parse("2028-02-29T12:00:00Z"),result.get(0).start());
        assertEquals(result.get(0).end(),result.get(1).start());
        assertEquals(Instant.parse("2028-03-14T12:00:00Z"),result.get(6).start());
        for(int i=0;i<result.size();i++) {assertEquals(i+1,result.get(i).number());if(i>0)assertFalse(result.get(i).start().isBefore(result.get(i-1).end()));}
    }
    @Test void yearBoundaryAndExactOperationalLimit() {
        var result=SchedulePlan.generate(rules(rule(1,"09:00","10:00")),LocalDate.of(2026,12,31),"UTC",1000,BEFORE);
        assertEquals(1000,result.size());assertEquals(Instant.parse("2027-01-04T09:00:00Z"),result.get(0).start());
        assertEquals(CourseFailure.Reason.GENERATION_LIMIT,assertThrows(CourseFailure.class,()->SchedulePlan.generate(rules(rule(1,"09:00","10:00")),LocalDate.of(2027,1,1),"UTC",1001,BEFORE)).reason());
    }
    @Test void duplicateAndOverlapRejectedButAdjacencyAccepted() {
        assertThrows(CourseFailure.class,()->rules(rule(1,"09:00","10:00"),rule(1,"09:00","10:00")));
        assertThrows(CourseFailure.class,()->rules(rule(1,"09:00","11:00"),rule(1,"10:00","12:00")));
        assertEquals(2,rules(rule(1,"10:00","11:00"),rule(1,"09:00","10:00")).size());
    }
    @Test void rejectsMalformedRules() {
        for(Object value:List.of(rule(0,"09:00","10:00"),rule(8,"09:00","10:00"),rule(1,"9:00","10:00"),rule(1,"09:00:00","10:00"),rule(1,"23:00","01:00"),rule(1,"09:00","09:00"),Map.of("dayOfWeek","1","startTime","09:00","endTime","10:00"),Map.of("dayOfWeek",1,"startTime","09:00","endTime","10:00","id","1")))
            assertThrows(CourseFailure.class,()->rules(value));
        assertThrows(CourseFailure.class,()->rules((Object)null));assertThrows(CourseFailure.class,()->rules());
        assertThrows(CourseFailure.class,()->SchedulePlan.parse(Map.of("rules",List.of(rule(1,"09:00","10:00")),"teacherId","1")));
    }
    @Test void ruleLimitAndCanonicalOrder() {
        var rows=new ArrayList<Object>();
        for(int i=0;i<100;i++) rows.add(rule(1,LocalTime.MIDNIGHT.plusMinutes(i).toString(),LocalTime.MIDNIGHT.plusMinutes(i+1).toString()));
        assertEquals(100,rules(rows.toArray()).size());rows.add(rule(2,"09:00","10:00"));
        assertThrows(CourseFailure.class,()->rules(rows.toArray()));
    }
    @Test void rejectsDstGapAndAmbiguityIncludingLaterOccurrence() {
        for(var date:List.of(LocalDate.of(2027,3,7),LocalDate.of(2027,10,31))) {
            String start=date.getMonthValue()==3?"02:15":"01:15";
            String end=date.getMonthValue()==3?"03:15":"02:15";
            assertEquals(CourseFailure.Reason.SCHEDULE_TIME,assertThrows(CourseFailure.class,()->SchedulePlan.generate(rules(rule(7,start,end)),date,"America/New_York",2,BEFORE)).reason());
            assertEquals(1,SchedulePlan.generate(rules(rule(7,start,end)),date,"America/New_York",1,BEFORE).size());
        }
    }
    @Test void strictNowCutoffDoesNotSkipPastOccurrences() {
        var date=LocalDate.of(2027,1,4);var rows=rules(rule(1,"09:00","10:00"));
        for(String now:List.of("2027-01-04T09:00:00Z","2027-01-04T09:00:01Z"))
            assertEquals(CourseFailure.Reason.SCHEDULE_TIME,assertThrows(CourseFailure.class,()->SchedulePlan.generate(rows,date,"UTC",2,Instant.parse(now))).reason());
        assertEquals(2,SchedulePlan.generate(rows,date,"UTC",2,Instant.parse("2027-01-04T08:59:59Z")).size());
    }
    @Test void rejectsInvalidStoredMetadataAndDateOverflow() {
        var rows=rules(rule(1,"09:00","10:00"));
        assertThrows(CourseFailure.class,()->SchedulePlan.generate(rows,LocalDate.of(2027,1,1),"+07:00",1,BEFORE));
        assertThrows(CourseFailure.class,()->SchedulePlan.generate(rows,LocalDate.of(2027,1,1),"UTC",0,BEFORE));
        assertThrows(CourseFailure.class,()->SchedulePlan.generate(rows,LocalDate.of(9999,12,31),"UTC",2,BEFORE));
    }
}

package com.englishlearning.features.course;

import com.englishlearning.features.course.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RescheduleInputTests {
    static Map<String,Object> fields(){return new HashMap<>(Map.of("expectedStartAt","2030-01-01T12:00:00Z","expectedEndAt","2030-01-01T13:00:00Z","scheduledStartAt","2030-01-02T12:00:00Z","scheduledEndAt","2030-01-02T13:00:00Z"));}
    static final Instant NOW=Instant.parse("2029-01-01T00:00:00Z");
    @Test void explicitOffsetsAndExpectedMicroseconds() {
        var f=fields();f.put("scheduledStartAt","2030-01-02T19:00:00+07:00");f.put("expectedStartAt","2030-01-01T12:00:00.123456Z");
        var input=RescheduleInput.parse(f,"Asia/Ho_Chi_Minh",NOW);assertEquals(Instant.parse("2030-01-02T12:00:00Z"),input.scheduledStartAt());
        assertEquals(123456000,input.expectedStartAt().getNano());
    }
    @Test void strictTimestampsAndFields() {
        for(Object value:List.of("2030-01-02T12:00:00","2030-01-02T12:00:00.0000000Z","2030-02-30T12:00:00Z","2030-01-02T12:00:00+0700",42)) {
            var f=fields();f.put("expectedStartAt",value);assertThrows(CourseFailure.class,()->RescheduleInput.parse(f,"UTC",NOW));
        }
        var f=fields();f.remove("expectedEndAt");assertThrows(CourseFailure.class,()->RescheduleInput.parse(f,"UTC",NOW));
        f.put("unexpected",1);assertThrows(CourseFailure.class,()->RescheduleInput.parse(f,"UTC",NOW));
        f.clear();f.putAll(fields());f.put("expectedStartAt",null);assertThrows(CourseFailure.class,()->RescheduleInput.parse(f,"UTC",NOW));
    }
    @Test void minutePrecisionSameLocalDateAndOrdering() {
        for(String end:List.of("2030-01-02T12:00:00Z","2030-01-02T11:00:00Z","2030-01-02T13:00:01Z","2030-01-02T13:00:00.000001Z","2030-01-02T18:00:00Z")) {
            var f=fields();f.put("scheduledEndAt",end);assertThrows(CourseFailure.class,()->RescheduleInput.parse(f,"Asia/Ho_Chi_Minh",NOW));
        }
        var f=fields();f.put("scheduledStartAt","2030-01-02T16:30:00Z");f.put("scheduledEndAt","2030-01-02T17:00:00Z");
        assertThrows(CourseFailure.class,()->RescheduleInput.parse(f,"Asia/Ho_Chi_Minh",NOW));
    }
    @Test void equalNowAndPastDenied() {
        for(String now:List.of("2030-01-02T12:00:00Z","2030-01-02T12:01:00Z"))assertEquals(CourseFailure.Reason.SCHEDULE_TIME,
                assertThrows(CourseFailure.class,()->RescheduleInput.parse(fields(),"UTC",Instant.parse(now))).reason());
    }
    @Test void cancellationTextRules() {
        assertNull(RescheduleInput.cancellationReason(Map.of()));assertNull(RescheduleInput.cancellationReason(Collections.singletonMap("reason",null)));
        assertEquals("Reason",RescheduleInput.cancellationReason(Map.of("reason","  Reason  ")));
        assertEquals("😀".repeat(1000),RescheduleInput.cancellationReason(Map.of("reason","😀".repeat(1000))));
        for(Object v:List.of(" ","a\n","\uD800","a\u0000","x".repeat(1001),42))assertThrows(CourseFailure.class,()->RescheduleInput.cancellationReason(Map.of("reason",v)));
        assertThrows(CourseFailure.class,()->RescheduleInput.cancellationReason(Map.of("status","CANCELLED")));
    }
}

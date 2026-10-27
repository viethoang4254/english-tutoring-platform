package com.englishlearning.features.course.service;

import java.time.*;
import java.util.*;

public record RescheduleInput(Instant expectedStartAt,Instant expectedEndAt,Instant scheduledStartAt,Instant scheduledEndAt) {
    public static RescheduleInput parse(Map<String,Object> fields,String timezone,Instant now) {
        if(fields==null || !fields.keySet().equals(Set.of("expectedStartAt","expectedEndAt","scheduledStartAt","scheduledEndAt")))throw CourseFailure.invalid();
        var value=new RescheduleInput(timestamp(fields.get("expectedStartAt")),timestamp(fields.get("expectedEndAt")),
                timestamp(fields.get("scheduledStartAt")),timestamp(fields.get("scheduledEndAt")));
        if(value.scheduledStartAt.getNano()!=0 || value.scheduledEndAt.getNano()!=0
                || Math.floorMod(value.scheduledStartAt.getEpochSecond(),60)!=0 || Math.floorMod(value.scheduledEndAt.getEpochSecond(),60)!=0
                || !value.scheduledEndAt.isAfter(value.scheduledStartAt))throw CourseFailure.invalid();
        try {
            var zone=ZoneId.of(timezone);
            var start=value.scheduledStartAt.atZone(zone);var end=value.scheduledEndAt.atZone(zone);
            if(!start.toLocalDate().equals(end.toLocalDate()) || start.getSecond()!=0 || end.getSecond()!=0)throw CourseFailure.invalid();
        } catch(DateTimeException e){throw CourseFailure.invalid();}
        if(!value.scheduledStartAt.isAfter(now))throw new CourseFailure(CourseFailure.Reason.SCHEDULE_TIME);
        return value;
    }
    private static Instant timestamp(Object value) {
        if(!(value instanceof String s) || !s.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]{1,6})?(?:Z|[+-][0-9]{2}:[0-9]{2})"))throw CourseFailure.invalid();
        try {
            var result=OffsetDateTime.parse(s).toInstant();int year=result.atOffset(ZoneOffset.UTC).getYear();
            if(year<1 || year>9999)throw CourseFailure.invalid();return result;
        } catch(DateTimeException e){throw CourseFailure.invalid();}
    }
    public static String cancellationReason(Map<String,Object> fields) {
        if(fields==null || !Set.of("reason").containsAll(fields.keySet()))throw CourseFailure.invalid();
        return fields.get("reason")==null?null:CourseInput.text(fields.get("reason"),1000,false);
    }
}

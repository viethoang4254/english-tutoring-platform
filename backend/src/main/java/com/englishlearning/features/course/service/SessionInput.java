package com.englishlearning.features.course.service;

import com.englishlearning.features.course.dto.SessionDto;
import java.util.Map;
import java.util.Set;

public record SessionInput(String title, String content) {
    public static SessionInput merge(Map<String,Object> fields, SessionDto current) {
        if (fields == null || fields.isEmpty() || !Set.of("title","content").containsAll(fields.keySet()))
            throw CourseFailure.invalid();
        return new SessionInput(fields.containsKey("title") ? nullableText(fields.get("title"),200,false) : current.title(),
                fields.containsKey("content") ? nullableText(fields.get("content"),10000,true) : current.content());
    }
    private static String nullableText(Object value,int limit,boolean multiline) {
        return value == null ? null : CourseInput.text(value,limit,multiline);
    }
}

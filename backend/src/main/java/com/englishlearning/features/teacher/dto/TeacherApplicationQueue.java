package com.englishlearning.features.teacher.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;
import java.util.List;

public record TeacherApplicationQueue(List<Item> items, int page, int size, boolean hasNext) {
    public TeacherApplicationQueue { items = List.copyOf(items); }
    public record Item(ApiId id, ApiId teacherId, String status, Instant submittedAt) {}
}

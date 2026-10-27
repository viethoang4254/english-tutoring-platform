package com.englishlearning.features.category.dto;
import com.englishlearning.common.api.ApiId;
import java.time.Instant;
public record CategoryDto(ApiId id, String name, boolean active, Instant createdAt, Instant updatedAt) {}

package com.englishlearning.features.user.dto;
import com.englishlearning.common.api.ApiId;
public record AdminUserDto(ApiId id, String fullName, String email, String role, boolean emailVerified, boolean locked) {}

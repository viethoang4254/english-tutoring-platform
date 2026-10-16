package com.englishlearning.features.user.dto;
import com.englishlearning.common.api.ApiId;
public record StudentProfile(ApiId id, String fullName, String username, String email, String phone, boolean emailVerified) {}

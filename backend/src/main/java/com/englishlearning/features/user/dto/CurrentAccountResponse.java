package com.englishlearning.features.user.dto;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.user.entity.User.Role;

public record CurrentAccountResponse(ApiId id, Role role, boolean emailVerified) {}

package com.englishlearning.features.paymentaccount.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;

public record ReceivingAccountReview(ApiId teacherId, ReceivingAccount account,
        ApiId reviewedBy, Instant reviewedAt, String rejectionReason) {}

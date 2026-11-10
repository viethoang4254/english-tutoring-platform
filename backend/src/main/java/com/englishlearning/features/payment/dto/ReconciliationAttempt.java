package com.englishlearning.features.payment.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;

public record ReconciliationAttempt(ApiId id, ApiId paymentId, ApiId teacherId,
        ReconciliationEvidence evidence, boolean late, Instant attemptedAt, String outcome,
        String failureReason, boolean allocated, ApiId replayOf) {}

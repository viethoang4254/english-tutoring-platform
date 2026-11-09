package com.englishlearning.features.payment.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;

public record PaymentDto(ApiId id,ApiId enrollmentId,String status,String currency,String originalAmount,
        String discountPercent,String exactDiscountAmount,String roundingAdjustment,String finalAmount,
        String transferReference,ApiId receivingAccountId,String bankBin,String bankName,
        String accountNumber,String accountHolderName,String qrPayload,Instant expiresAt,Instant createdAt) {}

package com.englishlearning.features.paymentaccount.dto;
import com.englishlearning.common.api.ApiId;
import java.time.Instant;
public record ReceivingAccount(ApiId id,String bankBin,String bankCode,String bankName,String holderName,
    String accountNumber,String reviewStatus,String selectionStatus,boolean hasQrImage,Instant createdAt) {}

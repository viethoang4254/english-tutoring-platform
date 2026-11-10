package com.englishlearning.features.payment.dto;

import com.englishlearning.features.payment.service.PaymentFailure;
import java.math.BigDecimal;
import java.time.Instant;

public record ReconciliationEvidence(String bankBin, String accountNumber, String bankTransactionReference,
        Instant receivedAt, BigDecimal observedAmount, String currency, String transferDescription,
        String transferReference, Boolean attested) {
    public ReconciliationEvidence validated() {
        if (bankBin == null || !bankBin.matches("[0-9]{6}") || accountNumber == null
                || !accountNumber.matches("[0-9]{6,19}") || receivedAt == null || observedAmount == null
                || observedAmount.scale() > 6 || observedAmount.abs().compareTo(new BigDecimal("10000000000000")) >= 0
                || attested == null || receivedAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                || receivedAt.isAfter(Instant.parse("9999-12-31T23:59:59Z"))) throw invalid();
        // Trim only surrounding ASCII spaces; preserve case, punctuation and leading zeroes.
        String reference = text(bankTransactionReference,128).replaceAll("^ +| +$", "");
        if (reference.isEmpty()) throw invalid();
        return new ReconciliationEvidence(bankBin,accountNumber,reference,receivedAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS),observedAmount,
                text(currency,8),text(transferDescription,1000),text(transferReference,128),attested);
    }
    private static String text(String value,int max) {
        if(value==null || value.isBlank() || value.length()>max || value.codePoints().anyMatch(Character::isISOControl)) throw invalid();
        return value;
    }
    private static PaymentFailure invalid() { return new PaymentFailure(PaymentFailure.Reason.INVALID_REQUEST); }
}

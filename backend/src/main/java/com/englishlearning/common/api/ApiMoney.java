package com.englishlearning.common.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;

// Representation only: operation-specific bounds, currency eligibility and rounding
// remain with the owning use case. No floating-point conversion or rounding occurs here.
public record ApiMoney(String amount, String currency) {
    @JsonCreator
    public ApiMoney {
        if (amount == null || !amount.matches("-?[0-9]+(\\.[0-9]+)?")
                || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Invalid money.");
        }
    }

    public static ApiMoney from(BigDecimal amount, String currency) {
        return new ApiMoney(amount.toPlainString(), currency);
    }

    public BigDecimal decimalAmount() {
        return new BigDecimal(amount);
    }
}

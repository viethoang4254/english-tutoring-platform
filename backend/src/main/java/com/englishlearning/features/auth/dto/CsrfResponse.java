package com.englishlearning.features.auth.dto;

public record CsrfResponse(String token) {
    @Override public String toString() { return "CsrfResponse[REDACTED]"; }
}

package com.englishlearning.features.auth.service;

import com.englishlearning.features.auth.security.AccessToken;

/** Internal committed credential pair; transport is a separate workflow decision. */
public record AuthenticationResult(AccessToken accessToken, RefreshCredential refreshCredential) {
    @Override public String toString() { return "AuthenticationResult[REDACTED]"; }
}

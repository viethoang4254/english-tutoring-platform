package com.englishlearning.features.auth.security;

import org.springframework.security.core.AuthenticationException;

// A filter-level failure, distinct from rejected credentials; never retain database details.
final class AccountLookupFailure extends AuthenticationException {
    private final boolean unavailable;

    AccountLookupFailure(boolean unavailable) {
        super("Account resolution failed.");
        this.unavailable = unavailable;
    }

    boolean isUnavailable() { return unavailable; }
}

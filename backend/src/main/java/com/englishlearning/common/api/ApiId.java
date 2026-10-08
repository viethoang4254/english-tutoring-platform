package com.englishlearning.common.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// Use only for identifier DTO fields, not counts or arbitrary Long values.
public record ApiId(long value) {
    public ApiId {
        if (value < 0) {
            throw new IllegalArgumentException("Invalid identifier.");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ApiId parse(String value) {
        if (value == null || !value.matches("[0-9]+")) {
            throw new IllegalArgumentException("Invalid identifier.");
        }
        return new ApiId(Long.parseLong(value));
    }

    @JsonValue
    public String decimal() {
        return Long.toString(value);
    }
}

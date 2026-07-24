package com.srm.platform.integration;

import java.util.Objects;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Idempotency key must contain 1 to 128 characters");
        }
    }
}


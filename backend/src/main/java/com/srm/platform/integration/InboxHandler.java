package com.srm.platform.integration;

public interface InboxHandler {

    boolean alreadyProcessed(String sourceSystem, IdempotencyKey idempotencyKey);

    void markProcessed(String sourceSystem, IdempotencyKey idempotencyKey);
}


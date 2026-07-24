package com.srm.platform.integration;

public interface OutboxPublisher {

    void publish(String eventType, String aggregateId, String payload, IdempotencyKey idempotencyKey);
}


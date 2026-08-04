package com.srm.platform.integration;

public record InboxEventPayload(
        String eventId,
        String sourceSystem,
        String objectType,
        String objectId,
        long objectVersion,
        String payload) {
}

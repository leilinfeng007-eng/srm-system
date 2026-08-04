package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record OutboxEventRequest(
        @NotBlank String eventId,
        @NotBlank String objectType,
        @NotBlank String objectId,
        @NotNull @PositiveOrZero Long objectVersion,
        @NotBlank String eventType,
        String payloadSummary,
        String traceId) {
}

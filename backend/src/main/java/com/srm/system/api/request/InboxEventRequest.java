package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InboxEventRequest(
        @NotBlank String eventId,
        @NotBlank String sourceSystem,
        @NotBlank String objectType,
        @NotBlank String objectId,
        @NotNull @PositiveOrZero Long objectVersion,
        @NotBlank String payload) {
}

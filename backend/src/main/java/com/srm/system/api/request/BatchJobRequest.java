package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;

public record BatchJobRequest(@NotBlank String jobType, @NotBlank String objectType,
                              @NotBlank String idempotencyKey) {}

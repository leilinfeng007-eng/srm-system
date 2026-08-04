package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BatchExportRequest(@NotBlank String objectType,
                                 @NotBlank @Size(max=80) String idempotencyKey) {
}

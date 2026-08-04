package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 200) String oldPassword,
        @NotBlank @Size(max = 200) String newPassword) {
}

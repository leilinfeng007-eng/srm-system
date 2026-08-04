package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;

public record ParameterVersionRequest(@NotBlank String paramValue) {}

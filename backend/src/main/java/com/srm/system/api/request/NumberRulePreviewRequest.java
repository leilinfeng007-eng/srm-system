package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;

public record NumberRulePreviewRequest(@NotBlank String ruleCode, Long orgId) {}

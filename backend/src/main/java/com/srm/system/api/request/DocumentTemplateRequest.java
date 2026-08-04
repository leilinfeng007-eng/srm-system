package com.srm.system.api.request;

import jakarta.validation.constraints.NotBlank;

public record DocumentTemplateRequest(@NotBlank String templateCode,
                                      @NotBlank String templateName,
                                      String purpose,
                                      @NotBlank String domainCode) {}

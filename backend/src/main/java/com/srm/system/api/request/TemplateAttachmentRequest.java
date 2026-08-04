package com.srm.system.api.request;

import jakarta.validation.constraints.NotNull;

public record TemplateAttachmentRequest(@NotNull Long attachmentId) {}

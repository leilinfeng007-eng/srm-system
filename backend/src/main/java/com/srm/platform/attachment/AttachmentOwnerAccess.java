package com.srm.platform.attachment;

public record AttachmentOwnerAccess(
        String domainCode,
        String dimensionCode,
        Long organizationId,
        Long ownerUserId,
        boolean globalOnly) {
}

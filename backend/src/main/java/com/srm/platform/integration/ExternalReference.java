package com.srm.platform.integration;

public record ExternalReference(
        String sourceSystem,
        String externalId,
        String externalVersion) {
}


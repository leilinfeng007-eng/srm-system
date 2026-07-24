package com.srm.security.api.response;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserPrincipalResponse user) {
}


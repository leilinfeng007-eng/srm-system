package com.srm.security.auth;

import com.srm.security.api.response.TokenResponse;

public record IssuedTokens(TokenResponse response, String refreshToken) {
}


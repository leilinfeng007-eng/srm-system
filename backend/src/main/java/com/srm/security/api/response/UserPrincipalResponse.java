package com.srm.security.api.response;

import com.srm.security.auth.SrmPrincipal;
import java.util.List;

public record UserPrincipalResponse(
        long userId,
        String username,
        String displayName,
        List<String> roles,
        List<String> permissions) {

    public static UserPrincipalResponse from(SrmPrincipal principal) {
        return new UserPrincipalResponse(
                principal.userId(),
                principal.username(),
                principal.displayName(),
                principal.roles(),
                principal.permissions());
    }
}


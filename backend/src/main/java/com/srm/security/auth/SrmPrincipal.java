package com.srm.security.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record SrmPrincipal(
        long userId,
        String username,
        String password,
        String displayName,
        String status,
        boolean mustChangePassword,
        List<String> roles,
        List<String> permissions,
        Long sessionId) implements UserDetails {

    public SrmPrincipal {
        roles = List.copyOf(roles);
        permissions = List.copyOf(permissions);
    }

    public SrmPrincipal withSessionId(long newSessionId) {
        return new SrmPrincipal(
                userId,
                username,
                password,
                displayName,
                status,
                mustChangePassword,
                roles,
                permissions,
                newSessionId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> roleAuthorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        List<SimpleGrantedAuthority> permissionAuthorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return java.util.stream.Stream.concat(roleAuthorities.stream(), permissionAuthorities.stream())
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return "ACTIVE".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }
}


package com.ayushsingh.doc_helper.core.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class FirebaseAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final String credentials;

    public FirebaseAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        setAuthenticated(true);
    }

    public FirebaseAuthenticationToken(String credentials) {
        super(null);
        this.principal = null;
        this.credentials = credentials;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}

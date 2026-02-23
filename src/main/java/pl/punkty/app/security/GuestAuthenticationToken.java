package pl.punkty.app.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class GuestAuthenticationToken extends AbstractAuthenticationToken {
    private final String principal;

    public GuestAuthenticationToken(String principal) {
        super(null);
        this.principal = principal;
        setAuthenticated(false);
    }

    public GuestAuthenticationToken(String principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
package pl.punkty.app.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class GuestAuthenticationProvider implements AuthenticationProvider {
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String principal = (authentication.getPrincipal() == null) ? "" : authentication.getPrincipal().toString();
        if (!"guest".equals(principal)) {
            throw new BadCredentialsException("Invalid guest token");
        }
        return new GuestAuthenticationToken(
            "guest",
            List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return GuestAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

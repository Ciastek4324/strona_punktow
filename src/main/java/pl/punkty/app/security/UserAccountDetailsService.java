package pl.punkty.app.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.punkty.app.model.UserAccount;
import pl.punkty.app.repo.UserAccountRepository;

@Service
public class UserAccountDetailsService implements UserDetailsService {
    private final UserAccountRepository repo;

    public UserAccountDetailsService(UserAccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = repo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(account.getUsername())
            .password(account.getPasswordHash())
            .roles(account.getRole())
            .build();
    }
}
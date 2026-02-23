package pl.punkty.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.punkty.app.model.UserAccount;
import pl.punkty.app.repo.ExcuseRepository;
import pl.punkty.app.repo.UserAccountRepository;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(UserAccountRepository userRepo,
                               ExcuseRepository excuseRepo,
                               PasswordEncoder encoder) {
        return args -> {
            excuseRepo.backfillNullStatus();

            if (userRepo.findByUsername("PWierzycki").isEmpty()) {
                UserAccount admin = new UserAccount();
                admin.setUsername("PWierzycki");
                admin.setPasswordHash(encoder.encode("Pep123"));
                admin.setRole("ADMIN");
                userRepo.save(admin);
            }

            if (userRepo.findByUsername("Admin").isEmpty()) {
                UserAccount admin2 = new UserAccount();
                admin2.setUsername("Admin");
                admin2.setPasswordHash(encoder.encode("Haslo1"));
                admin2.setRole("ADMIN");
                userRepo.save(admin2);
            } else {
                UserAccount admin2 = userRepo.findByUsername("Admin").get();
                admin2.setPasswordHash(encoder.encode("Haslo1"));
                admin2.setRole("ADMIN");
                userRepo.save(admin2);
            }

            if (userRepo.findByUsername("kleksikprezes123").isEmpty()) {
                UserAccount user = new UserAccount();
                user.setUsername("kleksikprezes123");
                user.setPasswordHash(encoder.encode("kleks123"));
                user.setRole("USER");
                userRepo.save(user);
            }
        };
    }
}
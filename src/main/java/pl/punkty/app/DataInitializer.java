package pl.punkty.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.punkty.app.model.UserAccount;
import pl.punkty.app.repo.ExcuseRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.UserAccountRepository;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(UserAccountRepository userRepo,
                               ExcuseRepository excuseRepo,
                               PersonRepository personRepo,
                               PasswordEncoder encoder) {
        return args -> {
            excuseRepo.backfillNullStatus();
            normalizePeople(personRepo);

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
                user.setRole("ADMIN");
                userRepo.save(user);
            } else {
                UserAccount user = userRepo.findByUsername("kleksikprezes123").get();
                user.setPasswordHash(encoder.encode("kleks123"));
                user.setRole("ADMIN");
                userRepo.save(user);
            }
        };
    }

    private void normalizePeople(PersonRepository personRepo) {
        var people = personRepo.findAll();
        boolean changed = false;
        for (var person : people) {
            String fixed = toAscii(person.getDisplayName());
            if (!fixed.equals(person.getDisplayName())) {
                person.setDisplayName(fixed);
                changed = true;
            }
        }
        if (changed) {
            personRepo.saveAll(people);
        }
    }

    private String toAscii(String input) {
        if (input == null) {
            return null;
        }
        return input
            .replace("\u0105", "a").replace("\u0107", "c").replace("\u0119", "e")
            .replace("\u0142", "l").replace("\u0144", "n").replace("\u00f3", "o")
            .replace("\u015b", "s").replace("\u017c", "z").replace("\u017a", "z")
            .replace("\u0104", "A").replace("\u0106", "C").replace("\u0118", "E")
            .replace("\u0141", "L").replace("\u0143", "N").replace("\u00d3", "O")
            .replace("\u015a", "S").replace("\u017b", "Z").replace("\u0179", "Z");
    }
}

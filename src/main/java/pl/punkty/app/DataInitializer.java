package pl.punkty.app;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.punkty.app.model.CurrentPoints;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.UserAccount;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.UserAccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class DataInitializer {
    private static final class NamePoint {
        private final String name;
        private final int points;

        private NamePoint(String name, int points) {
            this.name = name;
            this.points = points;
        }
    }

    @Bean
    CommandLineRunner seedData(UserAccountRepository userRepo,
                               PersonRepository personRepo,
                               CurrentPointsRepository pointsRepo,
                               PasswordEncoder encoder) {
        return args -> {
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

            List<NamePoint> desired = List.of(
                new NamePoint("Krzysztof Wierzycki", -8),
                new NamePoint("RafaĹ‚ Opoka", -6),
                new NamePoint("Wojciech Zelek", 49),
                new NamePoint("Antoni Gorcowski", 50),
                new NamePoint("Damian Sopata", 16),
                new NamePoint("Filip Wierzycki", 31),
                new NamePoint("Karol JeĹĽ", -7),
                new NamePoint("Krzysztof Florek", 23),
                new NamePoint("Marcel Smoter", 9),
                new NamePoint("Marcin Opoka", -3),
                new NamePoint("Nikodem FrÄ…czyk", -7),
                new NamePoint("PaweĹ‚ JeĹĽ", -4),
                new NamePoint("Sebastian Wierzycki", 35),
                new NamePoint("Szymon Zelek", 51),
                new NamePoint("Tomek Gancarczyk", 23),
                new NamePoint("Wiktor Wierzycki", 46),
                new NamePoint("Wojciech Bieniek", 14),
                new NamePoint("Daniel Nowak", 42),
                new NamePoint("Jakub Mucha", 47),
                new NamePoint("RadosĹ‚aw Sopata", 25),
                new NamePoint("StanisĹ‚aw Lubecki", 54),
                new NamePoint("Jan Migacz", -16),
                new NamePoint("Kacper Florek", 15),
                new NamePoint("Karol Klag", 1),
                new NamePoint("MichaĹ‚ Furtak", 49),
                new NamePoint("MichaĹ‚ Opoka", -5),
                new NamePoint("PaweĹ‚ Wierzycki", 22),
                new NamePoint("Sebastian Sopata", 9),
                new NamePoint("Szymon Mucha", 12)
            );

            List<Person> existingPeople = personRepo.findAll();
            boolean needsReset = existingPeople.size() != desired.size();
            if (!needsReset) {
                List<String> existingNames = existingPeople.stream()
                    .map(Person::getDisplayName)
                    .sorted()
                    .toList();
                List<String> desiredNames = desired.stream()
                    .map(np -> np.name)
                    .sorted()
                    .toList();
                needsReset = !existingNames.equals(desiredNames);
            }

            if (needsReset) {
                pointsRepo.deleteAll();
                personRepo.deleteAll();
                List<Person> people = new ArrayList<>();
                for (NamePoint np : desired) {
                    Person person = new Person();
                    person.setDisplayName(np.name);
                    people.add(person);
                }
                personRepo.saveAll(people);
            }

            Map<String, Person> peopleByName = personRepo.findAll().stream()
                .collect(Collectors.toMap(Person::getDisplayName, p -> p));

            pointsRepo.deleteAll();
            List<CurrentPoints> points = new ArrayList<>();
            for (NamePoint np : desired) {
                Person person = peopleByName.get(np.name);
                if (person == null) {
                    continue;
                }
                CurrentPoints cp = new CurrentPoints();
                cp.setPerson(person);
                cp.setPoints(np.points);
                points.add(cp);
            }
            pointsRepo.saveAll(points);
        };
    }
}
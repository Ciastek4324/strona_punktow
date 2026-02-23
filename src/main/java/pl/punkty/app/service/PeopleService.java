package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.PersonRepository;

import java.util.List;

@Service
public class PeopleService {
    private final PersonRepository personRepository;

    public PeopleService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public List<Person> getPeopleSorted() {
        return personRepository.findAll().stream()
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();
    }

    public void addPerson(String displayName, PersonRole role, int basePoints) {
        String name = sanitizeName(displayName);
        int points = sanitizePoints(basePoints);
        if (name.isEmpty()) {
            return;
        }
        if (personRepository.findByDisplayNameIgnoreCase(name).isPresent()) {
            return;
        }
        Person person = new Person();
        person.setDisplayName(name);
        person.setRole(role);
        person.setBasePoints(points);
        personRepository.save(person);
    }

    public void updatePerson(Long id, PersonRole role, int basePoints) {
        int points = sanitizePoints(basePoints);
        personRepository.findById(id).ifPresent(person -> {
            person.setRole(role);
            person.setBasePoints(points);
            personRepository.save(person);
        });
    }

    public void updateRole(Long id, PersonRole role) {
        personRepository.findById(id).ifPresent(person -> {
            person.setRole(role);
            personRepository.save(person);
        });
    }

    public void deletePerson(Long id) {
        personRepository.deleteById(id);
    }

    public void normalizeAllNamesToAscii() {
        List<Person> people = personRepository.findAll();
        boolean changed = false;
        for (Person person : people) {
            String fixed = sanitizeName(person.getDisplayName());
            if (!fixed.equals(person.getDisplayName())) {
                person.setDisplayName(fixed);
                changed = true;
            }
        }
        if (changed) {
            personRepository.saveAll(people);
        }
    }

    private int sanitizePoints(int points) {
        if (points < -9999 || points > 9999) {
            return 0;
        }
        return points;
    }

    private String sanitizeName(String value) {
        if (value == null) {
            return "";
        }
        String name = toAscii(value.trim());
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        return name;
    }

    private String toAscii(String input) {
        return input
            .replace("\u0105", "a").replace("\u0107", "c").replace("\u0119", "e")
            .replace("\u0142", "l").replace("\u0144", "n").replace("\u00f3", "o")
            .replace("\u015b", "s").replace("\u017c", "z").replace("\u017a", "z")
            .replace("\u0104", "A").replace("\u0106", "C").replace("\u0118", "E")
            .replace("\u0141", "L").replace("\u0143", "N").replace("\u00d3", "O")
            .replace("\u015a", "S").replace("\u017b", "Z").replace("\u0179", "Z");
    }
}

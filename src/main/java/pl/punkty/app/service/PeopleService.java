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
        String name = toAscii(fixMojibake(value.trim()));
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        return name;
    }

    private String fixMojibake(String input) {
        String out = input
            .replace("\u00c4\u2026", "a")  // Ä…
            .replace("\u00c4\u2021", "c")  // Ä‡ 
            .replace("\u00c4\u2122", "e")  // Ä™
            .replace("\u00c5\u201a", "l")  // Å‚
            .replace("\u00c5\u201e", "n")  // Å„
            .replace("\u00c3\u00b3", "o")  // Ã³
            .replace("\u00c5\u203a", "s")  // Å›
            .replace("\u00c5\u00bc", "z")  // Å¼
            .replace("\u00c5\u00ba", "z")  // Åº
            .replace("\u00c4\u201e", "A")  // Ä„
            .replace("\u00c4\u2020", "C")  // Ä†
            .replace("\u00c4\u02dc", "E")  // Ä˜
            .replace("\u00c5\u0081", "L")  // Å
            .replace("\u00c5\u0083", "N")  // Åƒ
            .replace("\u00c3\u201c", "O")  // Ã“
            .replace("\u00c5\u009a", "S")  // Åš
            .replace("\u00c5\u00bb", "Z")  // Å»
            .replace("\u00c5\u00b9", "Z")  // Å¹
            .replace("\u00c4\u0061\u00e2\u20ac\u0161", "l") // Äaâ€š
            .replace("\u00c4\u0061\u00c4\u02dd", "z") // ÄaÄ˝
            .replace("\u00c4\u2026\u00c4\u02dd", "z") // Ä…Ä˝
            .replace("\u00c4\u0061\u00c4\u02da", "z") // ÄaÄŠ
            .replace("\u00c4\u0192\u00e2\u20ac\u00a6", "a") // Ă„â€¦
            .replace("\u00c4\u201a\u00e2\u20ac\u00a6", "a") // Ä‚â€¦
            .replace("\u00c5\u00b8", "z"); // Å¸
        return out;
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

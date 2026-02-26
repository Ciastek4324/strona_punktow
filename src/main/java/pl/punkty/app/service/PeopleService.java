package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.PersonRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PeopleService {
    private final PersonRepository personRepository;
    private static final Map<String, String> KNOWN_NAME_FIXES = new LinkedHashMap<>();

    static {
        KNOWN_NAME_FIXES.put("Rafal Opoka", "Rafa\u0142 Opoka");
        KNOWN_NAME_FIXES.put("Karol Jez", "Karol Je\u017c");
        KNOWN_NAME_FIXES.put("Pawel Jez", "Pawe\u0142 Je\u017c");
        KNOWN_NAME_FIXES.put("Pawel Wierzycki", "Pawe\u0142 Wierzycki");
        KNOWN_NAME_FIXES.put("Nikodem Franczyk", "Nikodem Fr\u0105czyk");
        KNOWN_NAME_FIXES.put("Radoslaw Sopata", "Rados\u0142aw Sopata");
        KNOWN_NAME_FIXES.put("Stanislaw Lubecki", "Stanis\u0142aw Lubecki");
        KNOWN_NAME_FIXES.put("Michal Furtak", "Micha\u0142 Furtak");
        KNOWN_NAME_FIXES.put("Michal Opoka", "Micha\u0142 Opoka");
        KNOWN_NAME_FIXES.put("Wojciech Zelek", "Wojciech \u017belek");
        KNOWN_NAME_FIXES.put("Szymon Zelek", "Szymon \u017belek");

        KNOWN_NAME_FIXES.put("FrĂ„â€¦czyk", "Fr\u0105czyk");
        KNOWN_NAME_FIXES.put("JeĂ„Ä…Ă„Ëť", "Je\u017c");
        KNOWN_NAME_FIXES.put("PaweÄaâ€š", "Pawe\u0142");
        KNOWN_NAME_FIXES.put("MichaÄaâ€š", "Micha\u0142");
        KNOWN_NAME_FIXES.put("RadosÄaâ€šaw", "Rados\u0142aw");
        KNOWN_NAME_FIXES.put("RafaÄaâ€š", "Rafa\u0142");
        KNOWN_NAME_FIXES.put("StanisÄaâ€šaw", "Stanis\u0142aw");
        KNOWN_NAME_FIXES.put("Karol JeÄaÄ˝", "Karol Je\u017c");
        KNOWN_NAME_FIXES.put("Nikodem FrĂ„â€¦czyk", "Nikodem Fr\u0105czyk");
    }

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
        String name = value.trim();
        for (Map.Entry<String, String> entry : KNOWN_NAME_FIXES.entrySet()) {
            name = name.replace(entry.getKey(), entry.getValue());
        }
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        return name;
    }
}

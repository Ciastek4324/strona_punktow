package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.PersonRepository;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PeopleService {
    public enum AddPersonResult {
        ADDED,
        DUPLICATE,
        INVALID
    }

    public enum UpdatePersonResult {
        UPDATED,
        DUPLICATE,
        NOT_FOUND,
        INVALID
    }

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

    public AddPersonResult addPerson(String displayName, PersonRole role, int basePoints) {
        String name = sanitizeName(displayName);
        int points = sanitizePoints(basePoints);
        if (name.isEmpty()) {
            return AddPersonResult.INVALID;
        }
        if (existsByNormalizedName(name, null)) {
            return AddPersonResult.DUPLICATE;
        }
        Person person = new Person();
        person.setDisplayName(name);
        person.setRole(role);
        person.setBasePoints(points);
        personRepository.save(person);
        return AddPersonResult.ADDED;
    }

    public void updatePerson(Long id, PersonRole role, int basePoints) {
        updatePerson(id, null, role, basePoints);
    }

    public UpdatePersonResult updatePerson(Long id, String displayName, PersonRole role, int basePoints) {
        int points = sanitizePoints(basePoints);
        Person person = personRepository.findById(id).orElse(null);
        if (person == null) {
            return UpdatePersonResult.NOT_FOUND;
        }

        if (displayName != null) {
            String name = sanitizeName(displayName);
            if (name.isBlank()) {
                return UpdatePersonResult.INVALID;
            }
            if (existsByNormalizedName(name, id)) {
                return UpdatePersonResult.DUPLICATE;
            }
            person.setDisplayName(name);
        }

        person.setRole(role);
        person.setBasePoints(points);
        personRepository.save(person);
        return UpdatePersonResult.UPDATED;
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

    private boolean existsByNormalizedName(String candidate, Long ignoreId) {
        String candidateKey = normalizeNameKey(candidate);
        for (Person person : personRepository.findAll()) {
            if (ignoreId != null && ignoreId.equals(person.getId())) {
                continue;
            }
            if (normalizeNameKey(person.getDisplayName()).equals(candidateKey)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeNameKey(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(cleaned, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
}

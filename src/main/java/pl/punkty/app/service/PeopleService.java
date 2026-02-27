package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.MonthlyScheduleEntryRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.PointsHistoryRepository;
import pl.punkty.app.repo.WeeklyAttendanceRepository;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    private final CurrentPointsRepository currentPointsRepository;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final WeeklyAttendanceRepository weeklyAttendanceRepository;
    private final MonthlyScheduleEntryRepository monthlyScheduleEntryRepository;
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
        KNOWN_NAME_FIXES.put("Wojciech Zelek", "Wojciech \u017Belek");
        KNOWN_NAME_FIXES.put("Szymon Zelek", "Szymon \u017Belek");
        KNOWN_NAME_FIXES.put("Nikodem Farnczyk", "Nikodem Fr\u0105czyk");
        KNOWN_NAME_FIXES.put("Nikodem Fraczyk", "Nikodem Fr\u0105czyk");
        KNOWN_NAME_FIXES.put("Wojciech Zelki", "Wojciech \u017Belek");
        KNOWN_NAME_FIXES.put("Szymon Zelki", "Szymon \u017Belek");

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

    public PeopleService(PersonRepository personRepository,
                         CurrentPointsRepository currentPointsRepository,
                         PointsHistoryRepository pointsHistoryRepository,
                         WeeklyAttendanceRepository weeklyAttendanceRepository,
                         MonthlyScheduleEntryRepository monthlyScheduleEntryRepository) {
        this.personRepository = personRepository;
        this.currentPointsRepository = currentPointsRepository;
        this.pointsHistoryRepository = pointsHistoryRepository;
        this.weeklyAttendanceRepository = weeklyAttendanceRepository;
        this.monthlyScheduleEntryRepository = monthlyScheduleEntryRepository;
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
            // Allow saving role/points even if historical duplicates exist and the name is unchanged.
            if (!normalizeNameKey(person.getDisplayName()).equals(normalizeNameKey(name))
                && existsByNormalizedName(name, id)) {
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

    @Transactional
    public void deletePerson(Long id) {
        monthlyScheduleEntryRepository.deleteByPersonId(id);
        weeklyAttendanceRepository.deleteByPersonId(id);
        currentPointsRepository.deleteByPersonId(id);
        pointsHistoryRepository.deleteByPersonId(id);
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
        String name = repairMojibake(value.trim());
        if (looksBroken(name)) {
            for (Map.Entry<String, String> entry : KNOWN_NAME_FIXES.entrySet()) {
                name = name.replace(entry.getKey(), entry.getValue());
            }
        }
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        return name;
    }

    private String repairMojibake(String text) {
        String fixed = text;
        fixed = decodeIfBroken(fixed, Charset.forName("windows-1250"));
        fixed = decodeIfBroken(fixed, StandardCharsets.ISO_8859_1);
        return fixed;
    }

    private String decodeIfBroken(String text, Charset source) {
        if (!looksBroken(text)) {
            return text;
        }
        String decoded = new String(text.getBytes(source), StandardCharsets.UTF_8);
        return quality(decoded) >= quality(text) ? decoded : text;
    }

    private boolean looksBroken(String value) {
        return value.contains("Ã") || value.contains("Ä") || value.contains("Å")
            || value.contains("Ĺ") || value.contains("Ă") || value.contains("â")
            || value.contains("Â") || value.contains("�");
    }

    private int quality(String value) {
        int score = 0;
        for (char c : value.toCharArray()) {
            if ("ąćęłńóśźżĄĆĘŁŃÓŚŹŻ".indexOf(c) >= 0) {
                score += 3;
            }
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || ".,-():".indexOf(c) >= 0) {
                score += 1;
            }
        }
        if (looksBroken(value)) {
            score -= 10;
        }
        return score;
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

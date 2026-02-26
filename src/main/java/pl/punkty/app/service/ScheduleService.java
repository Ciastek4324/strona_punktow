package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.MonthlySchedule;
import pl.punkty.app.model.MonthlyScheduleEntry;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.MonthlyScheduleEntryRepository;
import pl.punkty.app.repo.MonthlyScheduleRepository;
import pl.punkty.app.repo.PersonRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScheduleService {
    private final MonthlyScheduleRepository scheduleRepository;
    private final MonthlyScheduleEntryRepository entryRepository;
    private final PersonRepository personRepository;

    private static final int ROLE_ASPIRANT = 1;
    private static final int ROLE_MINISTRANT = 2;
    private static final int ROLE_LEKTOR = 3;
    private static final int SUNDAY_R = 71;
    private static final int SUNDAY_S = 72;
    private static final int SUNDAY_P = 73;

    public ScheduleService(MonthlyScheduleRepository scheduleRepository,
                           MonthlyScheduleEntryRepository entryRepository,
                           PersonRepository personRepository) {
        this.scheduleRepository = scheduleRepository;
        this.entryRepository = entryRepository;
        this.personRepository = personRepository;
    }

    private static final List<String> WEEK_DAYS = List.of(
        "Poniedzialek",
        "Wtorek",
        "Sroda",
        "Czwartek",
        "Piatek",
        "Sobota"
    );

    public Map<String, List<String>> sundayData() {
        return sundayData(LocalDate.now());
    }

    public Map<String, List<String>> sundayData(LocalDate date) {
        Map<Integer, List<String>> slots = loadScheduleSlots(date);
        if (!slots.isEmpty()) {
            Map<String, List<String>> sunday = new LinkedHashMap<>();
            if (hasSundayRoleSlots(slots)) {
                sunday.put("PRYMARIA (aspiranci)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_R, ROLE_ASPIRANT), List.of()));
                sunday.put("PRYMARIA (ministranci)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_R, ROLE_MINISTRANT), List.of()));
                sunday.put("PRYMARIA (lektorzy)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_R, ROLE_LEKTOR), List.of()));
                sunday.put("SUMA (aspiranci)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_S, ROLE_ASPIRANT), List.of()));
                sunday.put("SUMA (ministranci)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_S, ROLE_MINISTRANT), List.of()));
                sunday.put("SUMA (lektorzy)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_S, ROLE_LEKTOR), List.of()));
                sunday.put("III MSZA (aspiranci)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_P, ROLE_ASPIRANT), List.of()));
                sunday.put("III MSZA (ministranci)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_P, ROLE_MINISTRANT), List.of()));
                sunday.put("III MSZA (lektorzy)", slots.getOrDefault(sundayRoleSlotCode(SUNDAY_P, ROLE_LEKTOR), List.of()));
            } else {
                fillLegacySundayFromCombinedSlots(sunday, slots);
            }
            return sunday;
        }
        return baseSundayData();
    }

    public Map<String, List<String>> sundayDataFromBase(LocalDate date) {
        return baseSundayData();
    }

    public Map<String, List<String>> weekdayMinistranci(LocalDate date) {
        Map<Integer, List<String>> slots = loadScheduleSlots(date);
        if (!slots.isEmpty()) {
            Map<String, List<String>> map = new LinkedHashMap<>();
            boolean roleSlots = hasRoleSlots(slots);
            for (int i = 0; i < WEEK_DAYS.size(); i++) {
                int day = i + 1;
                List<String> names = roleSlots
                    ? slots.getOrDefault(roleSlotCode(day, ROLE_MINISTRANT), List.of())
                    : slots.getOrDefault(day, List.of());
                map.put(WEEK_DAYS.get(i), names);
            }
            return map;
        }
        return shiftWeekday(baseWeekdayMinistranci(), monthOffsetFromBase(date));
    }

    public Map<String, List<String>> weekdayMinistranciFromBase(LocalDate date) {
        return shiftWeekday(baseWeekdayMinistranci(), monthOffsetFromBase(date));
    }

    public Map<String, List<String>> weekdayLektorzy(LocalDate date) {
        Map<Integer, List<String>> slots = loadScheduleSlots(date);
        if (!slots.isEmpty()) {
            Map<String, List<String>> map = new LinkedHashMap<>();
            boolean roleSlots = hasRoleSlots(slots);
            for (int i = 0; i < WEEK_DAYS.size(); i++) {
                int day = i + 1;
                List<String> names = roleSlots
                    ? slots.getOrDefault(roleSlotCode(day, ROLE_LEKTOR), List.of())
                    : List.of();
                map.put(WEEK_DAYS.get(i), names);
            }
            return map;
        }
        return shiftWeekday(baseWeekdayLektorzy(), monthOffsetFromBase(date));
    }

    public Map<String, List<String>> weekdayLektorzyFromBase(LocalDate date) {
        return shiftWeekday(baseWeekdayLektorzy(), monthOffsetFromBase(date));
    }

    private Map<String, List<String>> baseWeekdayAspiranci() {
        Map<String, List<String>> weekdayAspiranci = new LinkedHashMap<>();
        weekdayAspiranci.put("Poniedzialek", List.of("Rafał Opoka"));
        weekdayAspiranci.put("Wtorek", List.of("Wojciech Żelek"));
        weekdayAspiranci.put("Sroda", List.of("Krzysztof Wierzycki"));
        weekdayAspiranci.put("Czwartek", List.of("Rafał Opoka"));
        weekdayAspiranci.put("Piatek", List.of("Wojciech Żelek"));
        weekdayAspiranci.put("Sobota", List.of("Krzysztof Wierzycki"));
        return weekdayAspiranci;
    }

    public Map<String, List<String>> weekdayAspiranci(LocalDate date) {
        Map<Integer, List<String>> slots = loadScheduleSlots(date);
        if (!slots.isEmpty()) {
            Map<String, List<String>> map = new LinkedHashMap<>();
            boolean roleSlots = hasRoleSlots(slots);
            for (int i = 0; i < WEEK_DAYS.size(); i++) {
                int day = i + 1;
                List<String> names = roleSlots
                    ? slots.getOrDefault(roleSlotCode(day, ROLE_ASPIRANT), List.of())
                    : shiftWeekday(baseWeekdayAspiranci(), monthOffsetFromBase(date)).getOrDefault(WEEK_DAYS.get(i), List.of());
                map.put(WEEK_DAYS.get(i), names);
            }
            return map;
        }
        return shiftWeekday(baseWeekdayAspiranci(), monthOffsetFromBase(date));
    }

    public Map<String, List<String>> weekdayAspiranciFromBase(LocalDate date) {
        return shiftWeekday(baseWeekdayAspiranci(), monthOffsetFromBase(date));
    }

    public Map<Integer, Set<String>> scheduledByDay(LocalDate date) {
        Map<Integer, List<String>> slots = loadScheduleSlots(date);
        if (!slots.isEmpty()) {
            Map<Integer, Set<String>> byDay = new HashMap<>();
            boolean roleSlots = hasRoleSlots(slots);
            Map<String, List<String>> aspiranci = roleSlots ? Map.of() : shiftWeekday(baseWeekdayAspiranci(), monthOffsetFromBase(date));
            for (int i = 0; i < WEEK_DAYS.size(); i++) {
                int code = i + 1;
                Set<String> names = new HashSet<>();
                if (roleSlots) {
                    names.addAll(slots.getOrDefault(roleSlotCode(code, ROLE_ASPIRANT), List.of()));
                    names.addAll(slots.getOrDefault(roleSlotCode(code, ROLE_MINISTRANT), List.of()));
                    names.addAll(slots.getOrDefault(roleSlotCode(code, ROLE_LEKTOR), List.of()));
                } else {
                    names.addAll(slots.getOrDefault(code, List.of()));
                    names.addAll(aspiranci.getOrDefault(WEEK_DAYS.get(i), List.of()));
                }
                byDay.put(code, names);
            }
            return byDay;
        }

        Map<String, List<String>> ministranci = weekdayMinistranci(date);
        Map<String, List<String>> lektorzy = weekdayLektorzy(date);
        Map<String, List<String>> aspiranci = weekdayAspiranci(date);

        Map<Integer, Set<String>> byDay = new HashMap<>();
        for (int i = 0; i < WEEK_DAYS.size(); i++) {
            String day = WEEK_DAYS.get(i);
            Set<String> names = new HashSet<>();
            names.addAll(ministranci.getOrDefault(day, List.of()));
            names.addAll(lektorzy.getOrDefault(day, List.of()));
            names.addAll(aspiranci.getOrDefault(day, List.of()));
            byDay.put(i + 1, names);
        }
        return byDay;
    }

    public Map<Integer, Set<String>> scheduledSundaySlots() {
        return scheduledSundaySlots(LocalDate.now());
    }

    public Map<Integer, Set<String>> scheduledSundaySlots(LocalDate date) {
        Map<Integer, List<String>> slots = loadScheduleSlots(date);
        if (!slots.isEmpty()) {
            Map<Integer, Set<String>> bySlot = new HashMap<>();
            if (hasSundayRoleSlots(slots)) {
                bySlot.put(SUNDAY_R, new HashSet<>());
                bySlot.put(SUNDAY_S, new HashSet<>());
                bySlot.put(SUNDAY_P, new HashSet<>());
                bySlot.get(SUNDAY_R).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_R, ROLE_ASPIRANT), List.of()));
                bySlot.get(SUNDAY_R).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_R, ROLE_MINISTRANT), List.of()));
                bySlot.get(SUNDAY_R).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_R, ROLE_LEKTOR), List.of()));
                bySlot.get(SUNDAY_S).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_S, ROLE_ASPIRANT), List.of()));
                bySlot.get(SUNDAY_S).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_S, ROLE_MINISTRANT), List.of()));
                bySlot.get(SUNDAY_S).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_S, ROLE_LEKTOR), List.of()));
                bySlot.get(SUNDAY_P).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_P, ROLE_ASPIRANT), List.of()));
                bySlot.get(SUNDAY_P).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_P, ROLE_MINISTRANT), List.of()));
                bySlot.get(SUNDAY_P).addAll(slots.getOrDefault(sundayRoleSlotCode(SUNDAY_P, ROLE_LEKTOR), List.of()));
            } else {
                bySlot.put(SUNDAY_R, new HashSet<>(slots.getOrDefault(SUNDAY_R, List.of())));
                bySlot.put(SUNDAY_S, new HashSet<>(slots.getOrDefault(SUNDAY_S, List.of())));
                bySlot.put(SUNDAY_P, new HashSet<>(slots.getOrDefault(SUNDAY_P, List.of())));
            }
            return bySlot;
        }

        Map<Integer, Set<String>> bySlot = new HashMap<>();
        bySlot.put(SUNDAY_R, new HashSet<>());
        bySlot.put(SUNDAY_S, new HashSet<>());
        bySlot.put(SUNDAY_P, new HashSet<>());
        for (Map.Entry<String, List<String>> entry : sundayData(date).entrySet()) {
            int slot = sundaySlotFromKey(entry.getKey());
            if (slot == 0) {
                continue;
            }
            bySlot.get(slot).addAll(entry.getValue());
        }
        return bySlot;
    }

    private int sundaySlotFromKey(String key) {
        if (key.startsWith("PRYMARIA")) {
            return SUNDAY_R;
        }
        if (key.startsWith("SUMA")) {
            return SUNDAY_S;
        }
        if (key.startsWith("III MSZA")) {
            return SUNDAY_P;
        }
        return 0;
    }

    private int sundayRoleSlotFromKey(String key) {
        int mass = sundaySlotFromKey(key);
        if (mass == 0) {
            return 0;
        }
        if (key.contains("(aspiranci)")) {
            return sundayRoleSlotCode(mass, ROLE_ASPIRANT);
        }
        if (key.contains("(ministranci)")) {
            return sundayRoleSlotCode(mass, ROLE_MINISTRANT);
        }
        if (key.contains("(lektorzy)")) {
            return sundayRoleSlotCode(mass, ROLE_LEKTOR);
        }
        return 0;
    }

    private Map<String, List<String>> baseWeekdayMinistranci() {
        Map<String, List<String>> weekdayMinistranci = new LinkedHashMap<>();
        weekdayMinistranci.put("Poniedzialek", List.of("Nikodem Frączyk", "Krzysztof Florek"));
        weekdayMinistranci.put("Wtorek", List.of("Tomasz Gancarczyk", "Marcin Opoka"));
        weekdayMinistranci.put("Sroda", List.of("Damian Sopata", "Karol Jeż", "Paweł Jeż"));
        weekdayMinistranci.put("Czwartek", List.of("Szymon Żelek", "Antoni Gorcowski"));
        weekdayMinistranci.put("Piatek", List.of("Wojciech Bieniek", "Sebastian Wierzycki"));
        weekdayMinistranci.put("Sobota", List.of("Filip Wierzycki", "Wiktor Wierzycki", "Marcel Smoter"));
        return weekdayMinistranci;
    }

    private Map<String, List<String>> baseWeekdayLektorzy() {
        Map<String, List<String>> weekdayLektorzy = new LinkedHashMap<>();
        weekdayLektorzy.put("Poniedzialek", List.of("Kacper Florek", "Karol Klag"));
        weekdayLektorzy.put("Wtorek", List.of("Sebastian Sopata", "Radosław Sopata"));
        weekdayLektorzy.put("Sroda", List.of("Paweł Wierzycki", "Daniel Nowak"));
        weekdayLektorzy.put("Czwartek", List.of("Michał Furtak"));
        weekdayLektorzy.put("Piatek", List.of("Stanisław Lubecki", "Jan Migacz"));
        weekdayLektorzy.put("Sobota", List.of("Szymon Mucha", "Jakub Mucha"));
        return weekdayLektorzy;
    }

    private Map<String, List<String>> baseSundayData() {
        Map<String, List<String>> sunday = new LinkedHashMap<>();
        sunday.put("PRYMARIA (aspiranci)", List.of("Rafał Opoka"));
        sunday.put("PRYMARIA (ministranci)", List.of("Marcel Smoter", "Krzysztof Florek", "Marcin Opoka", "Tomasz Gancarczyk"));
        sunday.put("PRYMARIA (lektorzy)", List.of("Stanisław Lubecki", "Kacper Florek", "Michał Furtak"));
        sunday.put("SUMA (aspiranci)", List.of("Wojciech Żelek"));
        sunday.put("SUMA (ministranci)", List.of("Szymon Żelek", "Filip Wierzycki", "Wiktor Wierzycki", "Antoni Gorcowski", "Wojciech Bieniek"));
        sunday.put("SUMA (lektorzy)", List.of("Daniel Nowak", "Jakub Mucha", "Szymon Mucha", "Jan Migacz"));
        sunday.put("III MSZA (aspiranci)", List.of("Krzysztof Wierzycki"));
        sunday.put("III MSZA (ministranci)", List.of("Nikodem Frączyk", "Damian Sopata", "Karol Jeż", "Paweł Jeż"));
        sunday.put("III MSZA (lektorzy)", List.of("Paweł Wierzycki", "Sebastian Sopata", "Radosław Sopata", "Karol Klag"));
        return sunday;
    }

    private int monthOffsetFromBase(LocalDate date) {
        YearMonth base = YearMonth.of(2026, 2);
        YearMonth target = YearMonth.of(date.getYear(), date.getMonth());
        long monthsBetween = ChronoUnit.MONTHS.between(base, target);
        int offset = (int) (monthsBetween % 6);
        if (offset < 0) {
            offset += 6;
        }
        return offset;
    }

    private Map<String, List<String>> shiftWeekday(Map<String, List<String>> original, int offset) {
        Map<String, List<String>> shifted = new LinkedHashMap<>();
        for (String day : WEEK_DAYS) {
            shifted.put(day, List.of());
        }
        for (int i = 0; i < WEEK_DAYS.size(); i++) {
            String fromDay = WEEK_DAYS.get(i);
            String toDay = WEEK_DAYS.get((i + offset) % WEEK_DAYS.size());
            shifted.put(toDay, original.getOrDefault(fromDay, List.of()));
        }
        return shifted;
    }

    public Map<Integer, List<String>> loadScheduleSlots(LocalDate date) {
        MonthlySchedule schedule = ensureScheduleInitialized(date);
        List<MonthlyScheduleEntry> entries = entryRepository.findAllByScheduleOrderBySlotCodeAscPositionAsc(schedule);
        Map<Integer, List<String>> slots = new HashMap<>();
        for (MonthlyScheduleEntry entry : entries) {
            int slotCode = normalizeReadSlotCode(entry);
            slots.computeIfAbsent(slotCode, k -> new ArrayList<>())
                .add(entry.getPerson().getDisplayName());
        }
        return slots;
    }

    public Map<Integer, List<Long>> loadScheduleSlotIds(LocalDate date) {
        MonthlySchedule schedule = ensureScheduleInitialized(date);
        List<MonthlyScheduleEntry> entries = entryRepository.findAllByScheduleOrderBySlotCodeAscPositionAsc(schedule);
        Map<Integer, List<Long>> slots = new HashMap<>();
        for (MonthlyScheduleEntry entry : entries) {
            int slotCode = normalizeReadSlotCode(entry);
            slots.computeIfAbsent(slotCode, k -> new ArrayList<>())
                .add(entry.getPerson().getId());
        }
        return slots;
    }

    public MonthlySchedule loadOrCreateSchedule(LocalDate date) {
        YearMonth month = YearMonth.of(date.getYear(), date.getMonth());
        LocalDate monthDate = LocalDate.of(month.getYear(), month.getMonth(), 1);
        return scheduleRepository.findByMonthDate(monthDate)
            .orElseGet(() -> {
                MonthlySchedule created = new MonthlySchedule();
                created.setMonthDate(monthDate);
                return scheduleRepository.save(created);
            });
    }

    public void saveSchedule(LocalDate date, Map<Integer, List<Long>> slotToPeople) {
        MonthlySchedule schedule = loadOrCreateSchedule(date);
        entryRepository.deleteBySchedule(schedule);
        List<Person> people = personRepository.findAllById(
            slotToPeople.values().stream().flatMap(List::stream).toList()
        );
        Map<Long, Person> personMap = new HashMap<>();
        for (Person person : people) {
            personMap.put(person.getId(), person);
        }

        List<MonthlyScheduleEntry> toSave = new ArrayList<>();
        for (Map.Entry<Integer, List<Long>> entry : slotToPeople.entrySet()) {
            int slot = entry.getKey();
            int pos = 0;
            for (Long personId : entry.getValue()) {
                Person person = personMap.get(personId);
                if (person == null) {
                    continue;
                }
                MonthlyScheduleEntry ms = new MonthlyScheduleEntry();
                ms.setSchedule(schedule);
                ms.setPerson(person);
                ms.setSlotCode(slot);
                ms.setPosition(pos++);
                toSave.add(ms);
            }
        }
        if (!toSave.isEmpty()) {
            entryRepository.saveAll(toSave);
        }
    }

    private MonthlySchedule ensureScheduleInitialized(LocalDate date) {
        MonthlySchedule schedule = loadOrCreateSchedule(date);
        if (entryRepository.countBySchedule(schedule) > 0) {
            return schedule;
        }
        Map<Integer, List<Long>> defaults = buildShiftedFromPrevious(date);
        if (defaults.isEmpty()) {
            defaults = buildDefaultSlotIds(date);
        }
        if (defaults.isEmpty()) {
            return schedule;
        }
        saveSchedule(date, defaults);
        return schedule;
    }

    private Map<Integer, List<Long>> buildShiftedFromPrevious(LocalDate date) {
        YearMonth current = YearMonth.of(date.getYear(), date.getMonth());
        YearMonth previous = current.minusMonths(1);
        LocalDate prevDate = LocalDate.of(previous.getYear(), previous.getMonth(), 1);
        MonthlySchedule prevSchedule = scheduleRepository.findByMonthDate(prevDate).orElse(null);
        if (prevSchedule == null || entryRepository.countBySchedule(prevSchedule) == 0) {
            return Map.of();
        }
        List<MonthlyScheduleEntry> entries = entryRepository.findAllByScheduleOrderBySlotCodeAscPositionAsc(prevSchedule);
        Map<Integer, List<Long>> shifted = new LinkedHashMap<>();
        for (MonthlyScheduleEntry entry : entries) {
            int slot = entry.getSlotCode();
            int shiftedSlot = shiftWeekdaySlot(slot);
            shifted.computeIfAbsent(shiftedSlot, k -> new ArrayList<>()).add(entry.getPerson().getId());
        }
        return shifted;
    }

    private int shiftWeekdaySlot(int slot) {
        if (slot >= 1 && slot <= 6) {
            int shiftedDay = (slot % 6) + 1;
            return roleSlotCode(shiftedDay, ROLE_MINISTRANT);
        }
        if (slot >= 11 && slot <= 63) {
            int day = slot / 10;
            int role = slot % 10;
            int shiftedDay = (day % 6) + 1;
            return (shiftedDay * 10) + role;
        }
        return slot;
    }

    private Map<Integer, List<Long>> buildDefaultSlotIds(LocalDate date) {
        Map<String, Long> nameToId = new HashMap<>();
        for (Person person : personRepository.findAll()) {
            nameToId.put(person.getDisplayName(), person.getId());
        }

        int offset = monthOffsetFromBase(date);
        Map<String, List<String>> ministranci = shiftWeekday(baseWeekdayMinistranci(), offset);
        Map<String, List<String>> lektorzy = shiftWeekday(baseWeekdayLektorzy(), offset);
        Map<String, List<String>> aspiranciByDay = shiftWeekday(baseWeekdayAspiranci(), offset);

        Map<Integer, List<Long>> slots = new LinkedHashMap<>();
        for (int i = 0; i < WEEK_DAYS.size(); i++) {
            String day = WEEK_DAYS.get(i);
            int dayCode = i + 1;
            List<Long> aspIds = new ArrayList<>();
            addNames(aspIds, aspiranciByDay.getOrDefault(day, List.of()), nameToId);
            slots.put(roleSlotCode(dayCode, ROLE_ASPIRANT), aspIds);

            List<Long> minIds = new ArrayList<>();
            addNames(minIds, ministranci.getOrDefault(day, List.of()), nameToId);
            slots.put(roleSlotCode(dayCode, ROLE_MINISTRANT), minIds);

            List<Long> lekIds = new ArrayList<>();
            addNames(lekIds, lektorzy.getOrDefault(day, List.of()), nameToId);
            slots.put(roleSlotCode(dayCode, ROLE_LEKTOR), lekIds);
        }

        Map<String, List<String>> sunday = baseSundayData();
        Map<Integer, List<Long>> sundaySlots = new LinkedHashMap<>();
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_R, ROLE_ASPIRANT), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_R, ROLE_MINISTRANT), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_R, ROLE_LEKTOR), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_S, ROLE_ASPIRANT), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_S, ROLE_MINISTRANT), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_S, ROLE_LEKTOR), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_P, ROLE_ASPIRANT), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_P, ROLE_MINISTRANT), new ArrayList<>());
        sundaySlots.put(sundayRoleSlotCode(SUNDAY_P, ROLE_LEKTOR), new ArrayList<>());
        for (Map.Entry<String, List<String>> entry : sunday.entrySet()) {
            int slot = sundayRoleSlotFromKey(entry.getKey());
            if (slot == 0) {
                continue;
            }
            addNames(sundaySlots.get(slot), entry.getValue(), nameToId);
        }
        slots.putAll(sundaySlots);
        return slots;
    }

    private boolean hasRoleSlots(Map<Integer, List<String>> slots) {
        for (Integer key : slots.keySet()) {
            if (key != null && key >= 11 && key <= 63) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSundayRoleSlots(Map<Integer, List<String>> slots) {
        for (Integer key : slots.keySet()) {
            if (key != null && key >= 711 && key <= 733) {
                return true;
            }
        }
        return false;
    }

    private int roleSlotCode(int day, int role) {
        return (day * 10) + role;
    }

    private int sundayRoleSlotCode(int massSlot, int role) {
        return (massSlot * 10) + role;
    }

    private int normalizeReadSlotCode(MonthlyScheduleEntry entry) {
        int slot = entry.getSlotCode();
        if (slot >= SUNDAY_R && slot <= SUNDAY_P) {
            int role = roleFromPerson(entry.getPerson());
            return sundayRoleSlotCode(slot, role);
        }
        return slot;
    }

    private int roleFromPerson(Person person) {
        PersonRole role = person.getRole();
        if (role == PersonRole.ASPIRANT) {
            return ROLE_ASPIRANT;
        }
        if (role == PersonRole.LEKTOR) {
            return ROLE_LEKTOR;
        }
        return ROLE_MINISTRANT;
    }

    private void fillLegacySundayFromCombinedSlots(Map<String, List<String>> sunday, Map<Integer, List<String>> slots) {
        Map<String, List<String>> baseSunday = baseSundayData();
        fillLegacySundayMass(
            sunday,
            slots.getOrDefault(SUNDAY_R, List.of()),
            baseSunday.getOrDefault("PRYMARIA (aspiranci)", List.of()),
            baseSunday.getOrDefault("PRYMARIA (lektorzy)", List.of()),
            "PRYMARIA"
        );
        fillLegacySundayMass(
            sunday,
            slots.getOrDefault(SUNDAY_S, List.of()),
            baseSunday.getOrDefault("SUMA (aspiranci)", List.of()),
            baseSunday.getOrDefault("SUMA (lektorzy)", List.of()),
            "SUMA"
        );
        fillLegacySundayMass(
            sunday,
            slots.getOrDefault(SUNDAY_P, List.of()),
            baseSunday.getOrDefault("III MSZA (aspiranci)", List.of()),
            baseSunday.getOrDefault("III MSZA (lektorzy)", List.of()),
            "III MSZA"
        );
    }

    private void fillLegacySundayMass(Map<String, List<String>> sunday,
                                      List<String> allNames,
                                      List<String> baseAspirants,
                                      List<String> baseLectors,
                                      String massLabel) {
        Set<String> aspSet = new HashSet<>(baseAspirants);
        Set<String> lekSet = new HashSet<>(baseLectors);
        List<String> aspirants = new ArrayList<>();
        List<String> lectors = new ArrayList<>();
        List<String> ministrants = new ArrayList<>();
        for (String name : allNames) {
            if (aspSet.contains(name)) {
                aspirants.add(name);
            } else if (lekSet.contains(name)) {
                lectors.add(name);
            } else {
                ministrants.add(name);
            }
        }
        sunday.put(massLabel + " (aspiranci)", aspirants);
        sunday.put(massLabel + " (ministranci)", ministrants);
        sunday.put(massLabel + " (lektorzy)", lectors);
    }

    private void addNames(List<Long> ids, List<String> names, Map<String, Long> nameToId) {
        for (String name : names) {
            Long id = nameToId.get(name);
            if (id != null) {
                ids.add(id);
            }
        }
    }
}

package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.MonthlySchedule;
import pl.punkty.app.model.MonthlyScheduleEntry;
import pl.punkty.app.model.Person;
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
            sunday.put("PRYMARIA (aspiranci)", List.of());
            sunday.put("PRYMARIA (ministranci)", slots.getOrDefault(71, List.of()));
            sunday.put("PRYMARIA (lektorzy)", List.of());
            sunday.put("SUMA (aspiranci)", List.of());
            sunday.put("SUMA (ministranci)", slots.getOrDefault(72, List.of()));
            sunday.put("SUMA (lektorzy)", List.of());
            sunday.put("III MSZA (aspiranci)", List.of());
            sunday.put("III MSZA (ministranci)", slots.getOrDefault(73, List.of()));
            sunday.put("III MSZA (lektorzy)", List.of());
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
            for (int i = 0; i < WEEK_DAYS.size(); i++) {
                map.put(WEEK_DAYS.get(i), slots.getOrDefault(i + 1, List.of()));
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
            for (int i = 0; i < WEEK_DAYS.size(); i++) {
                map.put(WEEK_DAYS.get(i), List.of());
            }
            return map;
        }
        return shiftWeekday(baseWeekdayLektorzy(), monthOffsetFromBase(date));
    }

    public Map<String, List<String>> weekdayLektorzyFromBase(LocalDate date) {
        return shiftWeekday(baseWeekdayLektorzy(), monthOffsetFromBase(date));
    }

    public List<String> weekdayAspiranci() {
        return List.of("Krzysztof Wierzycki", "Rafal Opoka", "Wojciech Zelek");
    }

    public Map<Integer, Set<String>> scheduledByDay(LocalDate date) {
        Map<Integer, List<String>> slots = loadScheduleSlots(date);
        if (!slots.isEmpty()) {
            Map<Integer, Set<String>> byDay = new HashMap<>();
            for (int i = 0; i < WEEK_DAYS.size(); i++) {
                int code = i + 1;
                byDay.put(code, new HashSet<>(slots.getOrDefault(code, List.of())));
            }
            return byDay;
        }

        Map<String, List<String>> ministranci = weekdayMinistranci(date);
        Map<String, List<String>> lektorzy = weekdayLektorzy(date);
        Set<String> aspiranci = new HashSet<>(weekdayAspiranci());

        Map<Integer, Set<String>> byDay = new HashMap<>();
        for (int i = 0; i < WEEK_DAYS.size(); i++) {
            String day = WEEK_DAYS.get(i);
            Set<String> names = new HashSet<>();
            names.addAll(ministranci.getOrDefault(day, List.of()));
            names.addAll(lektorzy.getOrDefault(day, List.of()));
            names.addAll(aspiranci);
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
            bySlot.put(71, new HashSet<>(slots.getOrDefault(71, List.of())));
            bySlot.put(72, new HashSet<>(slots.getOrDefault(72, List.of())));
            bySlot.put(73, new HashSet<>(slots.getOrDefault(73, List.of())));
            return bySlot;
        }

        Map<Integer, Set<String>> bySlot = new HashMap<>();
        bySlot.put(71, new HashSet<>());
        bySlot.put(72, new HashSet<>());
        bySlot.put(73, new HashSet<>());
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
            return 71;
        }
        if (key.startsWith("SUMA")) {
            return 72;
        }
        if (key.startsWith("III MSZA")) {
            return 73;
        }
        return 0;
    }

    private Map<String, List<String>> baseWeekdayMinistranci() {
        Map<String, List<String>> weekdayMinistranci = new LinkedHashMap<>();
        weekdayMinistranci.put("Poniedzialek", List.of("Nikodem Franczyk", "Krzysztof Florek"));
        weekdayMinistranci.put("Wtorek", List.of("Tomasz Gancarczyk", "Marcin Opoka"));
        weekdayMinistranci.put("Sroda", List.of("Damian Sopata", "Karol Jez", "Pawel Jez"));
        weekdayMinistranci.put("Czwartek", List.of("Szymon Zelek", "Antoni Gorcowski"));
        weekdayMinistranci.put("Piatek", List.of("Wojciech Bieniek", "Sebastian Wierzycki"));
        weekdayMinistranci.put("Sobota", List.of("Filip Wierzycki", "Wiktor Wierzycki", "Marcel Smoter"));
        return weekdayMinistranci;
    }

    private Map<String, List<String>> baseWeekdayLektorzy() {
        Map<String, List<String>> weekdayLektorzy = new LinkedHashMap<>();
        weekdayLektorzy.put("Poniedzialek", List.of("Kacper Florek", "Karol Klag"));
        weekdayLektorzy.put("Wtorek", List.of("Sebastian Sopata", "Radoslaw Sopata"));
        weekdayLektorzy.put("Sroda", List.of("Pawel Wierzycki", "Daniel Nowak"));
        weekdayLektorzy.put("Czwartek", List.of("Michal Furtak"));
        weekdayLektorzy.put("Piatek", List.of("Stanislaw Lubecki", "Jan Migacz"));
        weekdayLektorzy.put("Sobota", List.of("Szymon Mucha", "Jakub Mucha"));
        return weekdayLektorzy;
    }

    private Map<String, List<String>> baseSundayData() {
        Map<String, List<String>> sunday = new LinkedHashMap<>();
        sunday.put("PRYMARIA (aspiranci)", List.of("Rafal Opoka"));
        sunday.put("PRYMARIA (ministranci)", List.of("Marcel Smoter", "Krzysztof Florek", "Marcin Opoka", "Tomasz Gancarczyk"));
        sunday.put("PRYMARIA (lektorzy)", List.of("Stanislaw Lubecki", "Kacper Florek", "Michal Furtak"));
        sunday.put("SUMA (aspiranci)", List.of("Wojciech Zelek"));
        sunday.put("SUMA (ministranci)", List.of("Szymon Zelek", "Filip Wierzycki", "Wiktor Wierzycki", "Antoni Gorcowski", "Wojciech Bieniek"));
        sunday.put("SUMA (lektorzy)", List.of("Daniel Nowak", "Jakub Mucha", "Szymon Mucha", "Jan Migacz"));
        sunday.put("III MSZA (aspiranci)", List.of("Krzysztof Wierzycki"));
        sunday.put("III MSZA (ministranci)", List.of("Nikodem Franczyk", "Damian Sopata", "Karol Jez", "Pawel Jez"));
        sunday.put("III MSZA (lektorzy)", List.of("Pawel Wierzycki", "Sebastian Sopata", "Radoslaw Sopata", "Karol Klag"));
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
            slots.computeIfAbsent(entry.getSlotCode(), k -> new ArrayList<>())
                .add(entry.getPerson().getDisplayName());
        }
        return slots;
    }

    public Map<Integer, List<Long>> loadScheduleSlotIds(LocalDate date) {
        MonthlySchedule schedule = ensureScheduleInitialized(date);
        List<MonthlyScheduleEntry> entries = entryRepository.findAllByScheduleOrderBySlotCodeAscPositionAsc(schedule);
        Map<Integer, List<Long>> slots = new HashMap<>();
        for (MonthlyScheduleEntry entry : entries) {
            slots.computeIfAbsent(entry.getSlotCode(), k -> new ArrayList<>())
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
        Map<Integer, List<Long>> defaults = buildDefaultSlotIds(date);
        if (defaults.isEmpty()) {
            return schedule;
        }
        saveSchedule(date, defaults);
        return schedule;
    }

    private Map<Integer, List<Long>> buildDefaultSlotIds(LocalDate date) {
        Map<String, Long> nameToId = new HashMap<>();
        for (Person person : personRepository.findAll()) {
            nameToId.put(person.getDisplayName(), person.getId());
        }

        int offset = monthOffsetFromBase(date);
        Map<String, List<String>> ministranci = shiftWeekday(baseWeekdayMinistranci(), offset);
        Map<String, List<String>> lektorzy = shiftWeekday(baseWeekdayLektorzy(), offset);
        List<String> aspiranci = weekdayAspiranci();

        Map<Integer, List<Long>> slots = new LinkedHashMap<>();
        for (int i = 0; i < WEEK_DAYS.size(); i++) {
            String day = WEEK_DAYS.get(i);
            List<Long> ids = new ArrayList<>();
            addNames(ids, ministranci.getOrDefault(day, List.of()), nameToId);
            addNames(ids, lektorzy.getOrDefault(day, List.of()), nameToId);
            addNames(ids, aspiranci, nameToId);
            slots.put(i + 1, ids);
        }

        Map<String, List<String>> sunday = baseSundayData();
        Map<Integer, List<Long>> sundaySlots = new LinkedHashMap<>();
        sundaySlots.put(71, new ArrayList<>());
        sundaySlots.put(72, new ArrayList<>());
        sundaySlots.put(73, new ArrayList<>());
        for (Map.Entry<String, List<String>> entry : sunday.entrySet()) {
            int slot = sundaySlotFromKey(entry.getKey());
            if (slot == 0) {
                continue;
            }
            addNames(sundaySlots.get(slot), entry.getValue(), nameToId);
        }
        slots.putAll(sundaySlots);
        return slots;
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

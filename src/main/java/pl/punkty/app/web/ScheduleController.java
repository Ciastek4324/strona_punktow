package pl.punkty.app.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import pl.punkty.app.model.Person;
import pl.punkty.app.service.PeopleService;
import pl.punkty.app.service.ScheduleService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ScheduleController {
    private static final List<Slot> SLOTS = List.of(
        new Slot("Poniedzialek - Aspiranci", 11),
        new Slot("Poniedzialek - Ministranci", 12),
        new Slot("Poniedzialek - Lektorzy", 13),
        new Slot("Wtorek - Aspiranci", 21),
        new Slot("Wtorek - Ministranci", 22),
        new Slot("Wtorek - Lektorzy", 23),
        new Slot("Sroda - Aspiranci", 31),
        new Slot("Sroda - Ministranci", 32),
        new Slot("Sroda - Lektorzy", 33),
        new Slot("Czwartek - Aspiranci", 41),
        new Slot("Czwartek - Ministranci", 42),
        new Slot("Czwartek - Lektorzy", 43),
        new Slot("Piatek - Aspiranci", 51),
        new Slot("Piatek - Ministranci", 52),
        new Slot("Piatek - Lektorzy", 53),
        new Slot("Sobota - Aspiranci", 61),
        new Slot("Sobota - Ministranci", 62),
        new Slot("Sobota - Lektorzy", 63),
        new Slot("Niedziela R", 71),
        new Slot("Niedziela S", 72),
        new Slot("Niedziela P", 73)
    );

    private final ScheduleService scheduleService;
    private final PeopleService peopleService;

    public ScheduleController(ScheduleService scheduleService, PeopleService peopleService) {
        this.scheduleService = scheduleService;
        this.peopleService = peopleService;
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String schedule(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Model model) {
        LocalDate effective = date == null ? LocalDate.now() : date;
        YearMonth month = YearMonth.of(effective.getYear(), effective.getMonth());
        LocalDate monthDate = LocalDate.of(month.getYear(), month.getMonth(), 1);

        Map<Integer, List<Long>> slotIds = normalizeSlots(scheduleService.loadScheduleSlotIds(monthDate));
        Map<Long, Person> personById = new LinkedHashMap<>();
        for (Person person : peopleService.getPeopleSorted()) {
            personById.put(person.getId(), person);
        }
        Map<Integer, List<PersonChip>> slotPeople = new LinkedHashMap<>();
        for (Slot slot : SLOTS) {
            List<PersonChip> list = new ArrayList<>();
            for (Long id : slotIds.getOrDefault(slot.code(), List.of())) {
                Person person = personById.get(id);
                if (person != null) {
                    list.add(new PersonChip(person.getDisplayName(), person.getId()));
                }
            }
            slotPeople.put(slot.code(), list);
        }

        List<Person> people = new ArrayList<>(personById.values());
        model.addAttribute("date", monthDate);
        model.addAttribute("dateParam", monthDate.toString());
        model.addAttribute("people", people);
        model.addAttribute("slots", SLOTS);
        model.addAttribute("slotPeople", slotPeople);
        return "schedule";
    }

    @PostMapping("/schedule/save")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Transactional
    public String saveSchedule(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam MultiValueMap<String, String> params) {
        Map<Integer, List<Long>> slotToPeople = new LinkedHashMap<>();
        for (String key : params.keySet()) {
            if (!key.startsWith("slot_")) {
                continue;
            }
            String codeStr = key.substring("slot_".length());
            int slotCode;
            try {
                slotCode = Integer.parseInt(codeStr);
            } catch (NumberFormatException ex) {
                continue;
            }
            List<String> values = params.get(key);
            if (values == null || values.isEmpty()) {
                continue;
            }
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                long personId;
                try {
                    personId = Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    continue;
                }
                slotToPeople.computeIfAbsent(slotCode, k -> new ArrayList<>()).add(personId);
            }
        }
        scheduleService.saveSchedule(date, slotToPeople);
        return "redirect:/schedule?date=" + date;
    }

    public record Slot(String label, int code) { }

    public record PersonChip(String name, Long id) { }

    private Map<Integer, List<Long>> normalizeSlots(Map<Integer, List<Long>> raw) {
        Map<Integer, List<Long>> normalized = new LinkedHashMap<>();
        boolean hasRoleSlots = raw.keySet().stream().anyMatch(k -> k != null && k >= 11 && k <= 63);
        if (hasRoleSlots) {
            return raw;
        }
        for (Map.Entry<Integer, List<Long>> entry : raw.entrySet()) {
            int slot = entry.getKey();
            int mapped = slot;
            if (slot >= 1 && slot <= 6) {
                mapped = (slot * 10) + 2;
            }
            normalized.computeIfAbsent(mapped, k -> new ArrayList<>()).addAll(entry.getValue());
        }
        return normalized;
    }
}

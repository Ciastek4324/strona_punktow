package pl.punkty.app.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.punkty.app.model.CurrentPoints;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.WeeklyAttendanceRepository;
import pl.punkty.app.repo.WeeklyTableRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class WeeklyController {
    private static final List<DayOfWeek> DAYS = List.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY
    );

    private final WeeklyTableRepository tableRepository;
    private final WeeklyAttendanceRepository attendanceRepository;
    private final CurrentPointsRepository currentPointsRepository;
    private final PersonRepository personRepository;

    public WeeklyController(WeeklyTableRepository tableRepository,
                            WeeklyAttendanceRepository attendanceRepository,
                            CurrentPointsRepository currentPointsRepository,
                            PersonRepository personRepository) {
        this.tableRepository = tableRepository;
        this.attendanceRepository = attendanceRepository;
        this.currentPointsRepository = currentPointsRepository;
        this.personRepository = personRepository;
    }

    @GetMapping("/weekly")
    public String weekly(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                         Model model) {
        LocalDate weekStart = (week == null) ? LocalDate.now() : week;
        weekStart = weekStart.with(DayOfWeek.MONDAY);
        final LocalDate finalWeekStart = weekStart;
        WeeklyTable table = tableRepository.findByWeekStart(weekStart)
            .orElseGet(() -> {
                WeeklyTable created = new WeeklyTable();
                created.setWeekStart(finalWeekStart);
                return tableRepository.save(created);
            });

        List<Person> people = currentPointsRepository.findAll().stream()
            .map(CurrentPoints::getPerson)
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();
        if (people.isEmpty()) {
            people = personRepository.findAll().stream()
                .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
                .toList();
        }

        Map<Long, Set<DayOfWeek>> presence = new LinkedHashMap<>();
        List<WeeklyAttendance> existing = attendanceRepository.findByTableRef(table);
        for (WeeklyAttendance wa : existing) {
            presence.computeIfAbsent(wa.getPerson().getId(), k -> new java.util.HashSet<>())
                .add(dayFromInt(wa.getDayOfWeek()));
        }

        model.addAttribute("people", people);
        model.addAttribute("presence", presence);
        model.addAttribute("days", DAYS);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("prevWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        model.addAttribute("weekLabel", weekLabel(weekStart));

        return "weekly";
    }

    @PostMapping("/weekly/save")
    public String weeklySave(@RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                             @RequestParam Map<String, String> params) {
        WeeklyTable table = tableRepository.findByWeekStart(weekStart)
            .orElseGet(() -> {
                WeeklyTable created = new WeeklyTable();
                created.setWeekStart(weekStart);
                return tableRepository.save(created);
            });

        attendanceRepository.deleteByTableRef(table);

        Map<Long, List<DayOfWeek>> selected = new LinkedHashMap<>();
        for (String key : params.keySet()) {
            if (!key.startsWith("p_")) {
                continue;
            }
            String[] parts = key.split("_");
            if (parts.length != 3) {
                continue;
            }
            Long personId = Long.parseLong(parts[1]);
            DayOfWeek day = DayOfWeek.of(Integer.parseInt(parts[2]));
            selected.computeIfAbsent(personId, k -> new ArrayList<>()).add(day);
        }

        List<Person> people = personRepository.findAllById(selected.keySet());
        Map<Long, Person> personMap = people.stream().collect(Collectors.toMap(Person::getId, p -> p));

        List<WeeklyAttendance> toSave = new ArrayList<>();
        for (Map.Entry<Long, List<DayOfWeek>> entry : selected.entrySet()) {
            Person person = personMap.get(entry.getKey());
            if (person == null) {
                continue;
            }
            for (DayOfWeek day : entry.getValue()) {
                WeeklyAttendance wa = new WeeklyAttendance();
                wa.setTableRef(table);
                wa.setPerson(person);
                wa.setDayOfWeek(day.getValue());
                wa.setPresent(true);
                toSave.add(wa);
            }
        }
        attendanceRepository.saveAll(toSave);

        return "redirect:/weekly?week=" + weekStart;
    }

    private String weekLabel(LocalDate weekStart) {
        LocalDate end = weekStart.plusDays(5);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return fmt.format(weekStart) + " - " + fmt.format(end);
    }

    private DayOfWeek dayFromInt(int value) {
        if (value < 1 || value > 7) {
            return DayOfWeek.MONDAY;
        }
        return DayOfWeek.of(value);
    }
}

package pl.punkty.app.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import pl.punkty.app.model.CurrentPoints;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PointsSnapshot;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.PointsSnapshotRepository;
import pl.punkty.app.repo.WeeklyAttendanceRepository;
import pl.punkty.app.repo.WeeklyTableRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class WeeklyController {
    private static final List<WeekSlot> SLOTS = List.of(
        new WeekSlot("Pon", 1),
        new WeekSlot("Wt", 2),
        new WeekSlot("Sr", 3),
        new WeekSlot("Czw", 4),
        new WeekSlot("Pt", 5),
        new WeekSlot("Sb", 6),
        new WeekSlot("R", 71),
        new WeekSlot("S", 72),
        new WeekSlot("P", 73)
    );

    private final WeeklyTableRepository tableRepository;
    private final WeeklyAttendanceRepository attendanceRepository;
    private final CurrentPointsRepository currentPointsRepository;
    private final PersonRepository personRepository;
    private final PointsSnapshotRepository pointsSnapshotRepository;

    public WeeklyController(WeeklyTableRepository tableRepository,
                            WeeklyAttendanceRepository attendanceRepository,
                            CurrentPointsRepository currentPointsRepository,
                            PersonRepository personRepository,
                            PointsSnapshotRepository pointsSnapshotRepository) {
        this.tableRepository = tableRepository;
        this.attendanceRepository = attendanceRepository;
        this.currentPointsRepository = currentPointsRepository;
        this.personRepository = personRepository;
        this.pointsSnapshotRepository = pointsSnapshotRepository;
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

        Map<Long, Set<Integer>> presence = new LinkedHashMap<>();
        Map<Long, Integer> otherCounts = new LinkedHashMap<>();
        List<WeeklyAttendance> existing = attendanceRepository.findByTableRef(table);
        for (WeeklyAttendance wa : existing) {
            if (wa.getDayOfWeek() == 0) {
                otherCounts.put(wa.getPerson().getId(), wa.getOtherCount());
            } else {
                presence.computeIfAbsent(wa.getPerson().getId(), k -> new HashSet<>())
                    .add(wa.getDayOfWeek());
            }
        }

        model.addAttribute("people", people);
        model.addAttribute("presence", presence);
        model.addAttribute("otherCounts", otherCounts);
        model.addAttribute("slots", SLOTS);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("prevWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        model.addAttribute("weekLabel", weekLabel(weekStart));
        model.addAttribute("completed", table.isCompleted());

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

        Map<Long, List<Integer>> selected = new LinkedHashMap<>();
        Map<Long, Integer> otherSelected = new LinkedHashMap<>();
        for (String key : params.keySet()) {
            if (!key.startsWith("p_")) {
                if (key.startsWith("other_")) {
                    String[] parts = key.split("_");
                    if (parts.length != 2) {
                        continue;
                    }
                    Long personId = Long.parseLong(parts[1]);
                    int value;
                    try {
                        value = Integer.parseInt(params.get(key));
                    } catch (NumberFormatException ex) {
                        value = 0;
                    }
                    if (value < 0) {
                        value = 0;
                    }
                    if (value > 99) {
                        value = 99;
                    }
                    otherSelected.put(personId, value);
                }
                continue;
            }
            String[] parts = key.split("_");
            if (parts.length != 3) {
                continue;
            }
            Long personId = Long.parseLong(parts[1]);
            int slotCode = Integer.parseInt(parts[2]);
            selected.computeIfAbsent(personId, k -> new ArrayList<>()).add(slotCode);
        }

        List<Person> people = personRepository.findAllById(selected.keySet());
        Map<Long, Person> personMap = people.stream().collect(Collectors.toMap(Person::getId, p -> p));

        List<WeeklyAttendance> toSave = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : selected.entrySet()) {
            Person person = personMap.get(entry.getKey());
            if (person == null) {
                continue;
            }
            for (Integer slotCode : entry.getValue()) {
                WeeklyAttendance wa = new WeeklyAttendance();
                wa.setTableRef(table);
                wa.setPerson(person);
                wa.setDayOfWeek(slotCode);
                wa.setPresent(true);
                wa.setOtherCount(0);
                toSave.add(wa);
            }
        }

        for (Map.Entry<Long, Integer> entry : otherSelected.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            Person person = personMap.get(entry.getKey());
            if (person == null) {
                continue;
            }
            WeeklyAttendance wa = new WeeklyAttendance();
            wa.setTableRef(table);
            wa.setPerson(person);
            wa.setDayOfWeek(0);
            wa.setPresent(false);
            wa.setOtherCount(entry.getValue());
            toSave.add(wa);
        }
        attendanceRepository.saveAll(toSave);

        return "redirect:/weekly?week=" + weekStart;
    }

    @PostMapping("/weekly/reset")
    @Transactional
    public String weeklyReset(@RequestParam(value = "weekStart", required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        if (weekStart == null) {
            return "redirect:/weekly";
        }
        try {
            tableRepository.findByWeekStart(weekStart).ifPresent(table -> {
                attendanceRepository.deleteByTableRef(table);
                table.setCompleted(false);
                tableRepository.save(table);
            });
        } catch (Exception ex) {
            return "redirect:/weekly?week=" + weekStart + "&reset=error";
        }
        return "redirect:/weekly?week=" + weekStart + "&reset=ok";
    }

    @PostMapping("/weekly/complete")
    @Transactional
    public String weeklyComplete(@RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        WeeklyTable table = tableRepository.findByWeekStart(weekStart)
            .orElseGet(() -> {
                WeeklyTable created = new WeeklyTable();
                created.setWeekStart(weekStart);
                return tableRepository.save(created);
            });
        if (attendanceRepository.findByTableRef(table).isEmpty()) {
            return "redirect:/weekly?week=" + weekStart + "&complete=empty";
        }
        table.setCompleted(true);
        tableRepository.save(table);
        return "redirect:/weekly?week=" + weekStart + "&complete=ok";
    }

    @PostMapping("/weekly/uncomplete")
    @Transactional
    public String weeklyUncomplete(@RequestParam("weekStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        tableRepository.findByWeekStart(weekStart).ifPresent(table -> {
            table.setCompleted(false);
            tableRepository.save(table);
        });
        return "redirect:/weekly?week=" + weekStart;
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year, Model model) {
        int selectedYear = (year == null) ? LocalDate.now().getYear() : year;
        LocalDate yearStart = LocalDate.of(selectedYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(selectedYear, 12, 31);
        LocalDate firstWeekStart = yearStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<WeeklyTable> tables = tableRepository.findAllByWeekStartBetween(firstWeekStart, yearEnd);
        Map<Long, LocalDate> tableIdToWeek = new HashMap<>();
        for (WeeklyTable table : tables) {
            tableIdToWeek.put(table.getId(), table.getWeekStart());
        }

        Set<LocalDate> completedWeeks = new HashSet<>();
        for (WeeklyTable table : tables) {
            if (table.isCompleted()) {
                completedWeeks.add(table.getWeekStart());
            }
        }

        Optional<PointsSnapshot> snapshot = pointsSnapshotRepository.findTopByOrderBySnapshotDateDesc();
        LocalDate baselineDate = snapshot.map(PointsSnapshot::getSnapshotDate).orElse(null);

        List<WeekStatus> weeks = new ArrayList<>();
        for (LocalDate weekStart = firstWeekStart; !weekStart.isAfter(yearEnd); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(5);
            boolean complete = completedWeeks.contains(weekStart);
            if (baselineDate != null && !weekStart.isAfter(baselineDate)) {
                complete = true;
            }
            weeks.add(new WeekStatus(weekStart, weekEnd, complete));
        }

        model.addAttribute("weeks", weeks);
        model.addAttribute("year", selectedYear);
        model.addAttribute("baselineDate", baselineDate);
        model.addAttribute("prevYear", selectedYear - 1);
        model.addAttribute("nextYear", selectedYear + 1);
        return "calendar";
    }

    private String weekLabel(LocalDate weekStart) {
        LocalDate end = weekStart.plusDays(5);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return fmt.format(weekStart) + " - " + fmt.format(end);
    }

    public record WeekSlot(String label, int code) { }

    public record WeekStatus(LocalDate weekStart, LocalDate weekEnd, boolean complete) { }
}

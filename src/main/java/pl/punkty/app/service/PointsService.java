package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.WeeklyAttendanceRepository;
import pl.punkty.app.repo.WeeklyTableRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PointsService {
    private final WeeklyTableRepository weeklyTableRepository;
    private final WeeklyAttendanceRepository weeklyAttendanceRepository;
    private final PeopleService peopleService;
    private final ScheduleService scheduleService;

    public PointsService(WeeklyTableRepository weeklyTableRepository,
                         WeeklyAttendanceRepository weeklyAttendanceRepository,
                         PeopleService peopleService,
                         ScheduleService scheduleService) {
        this.weeklyTableRepository = weeklyTableRepository;
        this.weeklyAttendanceRepository = weeklyAttendanceRepository;
        this.peopleService = peopleService;
        this.scheduleService = scheduleService;
    }

    public Map<Long, Integer> monthPoints(LocalDate date) {
        YearMonth month = YearMonth.of(date.getYear(), date.getMonth());
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<WeeklyTable> tables = weeklyTableRepository.findAllByWeekStartBetween(start.minusDays(7), end);
        Map<Long, Integer> points = new LinkedHashMap<>();
        if (tables.isEmpty()) {
            return points;
        }
        Map<String, Long> nameToId = new HashMap<>();
        Map<Long, String> idToName = new HashMap<>();
        for (Person person : peopleService.getPeopleSorted()) {
            nameToId.put(person.getDisplayName(), person.getId());
            idToName.put(person.getId(), person.getDisplayName());
        }

        Map<Long, List<WeeklyAttendance>> byTable = new HashMap<>();
        for (WeeklyAttendance attendance : weeklyAttendanceRepository.findByTableRefIn(tables)) {
            byTable.computeIfAbsent(attendance.getTableRef().getId(), k -> new java.util.ArrayList<>())
                .add(attendance);
        }

        for (WeeklyTable table : tables) {
            LocalDate weekStart = table.getWeekStart();
            if (weekStart == null || weekStart.isBefore(start.minusDays(7)) || weekStart.isAfter(end)) {
                continue;
            }
            Map<Integer, Set<String>> sundayScheduled = scheduleService.scheduledSundaySlots();

            Map<Long, Set<Integer>> presentByPerson = new HashMap<>();
            List<WeeklyAttendance> attendances = byTable.getOrDefault(table.getId(), List.of());
            for (WeeklyAttendance attendance : attendances) {
                Long pid = attendance.getPerson().getId();
                int day = attendance.getDayOfWeek();
                if (day == 0) {
                    // ignore "Inne" for points
                } else {
                    presentByPerson.computeIfAbsent(pid, k -> new HashSet<>()).add(day);
                }
            }

            for (Map.Entry<Integer, Set<String>> entry : sundayScheduled.entrySet()) {
                int slot = entry.getKey();
                for (String name : entry.getValue()) {
                    Long pid = nameToId.get(name);
                    if (pid == null) {
                        continue;
                    }
                    boolean present = presentByPerson.getOrDefault(pid, Set.of()).contains(slot);
                    points.put(pid, points.getOrDefault(pid, 0) + (present ? 1 : -5));
                }
            }

            for (Map.Entry<Long, Set<Integer>> entry : presentByPerson.entrySet()) {
                Long pid = entry.getKey();
                String name = idToName.get(pid);
                if (name == null) {
                    continue;
                }
                for (Integer day : entry.getValue()) {
                    if (day == 71 || day == 72 || day == 73) {
                        boolean scheduledForSlot = sundayScheduled.getOrDefault(day, Set.of()).contains(name);
                        if (!scheduledForSlot) {
                            points.put(pid, points.getOrDefault(pid, 0) + 3);
                        }
                    }
                }
            }

            // weekdays and "Inne" do not change points
        }
        return points;
    }
}

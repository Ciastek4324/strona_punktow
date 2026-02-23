package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.WeeklyAttendanceRepository;
import pl.punkty.app.repo.WeeklyTableRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PointsService {
    private final WeeklyTableRepository weeklyTableRepository;
    private final WeeklyAttendanceRepository weeklyAttendanceRepository;

    public PointsService(WeeklyTableRepository weeklyTableRepository,
                         WeeklyAttendanceRepository weeklyAttendanceRepository) {
        this.weeklyTableRepository = weeklyTableRepository;
        this.weeklyAttendanceRepository = weeklyAttendanceRepository;
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
        for (WeeklyAttendance attendance : weeklyAttendanceRepository.findByTableRefIn(tables)) {
            LocalDate weekStart = attendance.getTableRef().getWeekStart();
            if (weekStart == null || weekStart.isBefore(start.minusDays(7)) || weekStart.isAfter(end)) {
                continue;
            }
            Long personId = attendance.getPerson().getId();
            points.put(personId, points.getOrDefault(personId, 0) + 1);
        }
        return points;
    }
}

package pl.punkty.app.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.WeeklyAttendanceRepository;
import pl.punkty.app.repo.WeeklyTableRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PointsServiceTest {
    @Test
    void monthPointsCountsAttendancesInsideMonth() throws Exception {
        WeeklyTableRepository tableRepo = Mockito.mock(WeeklyTableRepository.class);
        WeeklyAttendanceRepository attendanceRepo = Mockito.mock(WeeklyAttendanceRepository.class);
        PointsService service = new PointsService(tableRepo, attendanceRepo);

        WeeklyTable table = new WeeklyTable();
        table.setWeekStart(LocalDate.of(2026, 2, 2));

        Person person = new Person();
        setId(person, 10L);

        WeeklyAttendance attendance = new WeeklyAttendance();
        attendance.setTableRef(table);
        attendance.setPerson(person);
        attendance.setDayOfWeek(1);
        attendance.setPresent(true);

        when(tableRepo.findAllByWeekStartBetween(Mockito.any(), Mockito.any()))
            .thenReturn(List.of(table));
        when(attendanceRepo.findByTableRefIn(List.of(table)))
            .thenReturn(List.of(attendance));

        Map<Long, Integer> result = service.monthPoints(LocalDate.of(2026, 2, 5));
        assertEquals(1, result.get(10L));
    }

    private static void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}

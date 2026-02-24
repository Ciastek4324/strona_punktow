package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.MonthlySchedule;
import pl.punkty.app.model.MonthlyScheduleEntry;

import java.util.List;

public interface MonthlyScheduleEntryRepository extends JpaRepository<MonthlyScheduleEntry, Long> {
    List<MonthlyScheduleEntry> findAllByScheduleOrderBySlotCodeAscPositionAsc(MonthlySchedule schedule);
    void deleteBySchedule(MonthlySchedule schedule);
    long countBySchedule(MonthlySchedule schedule);
}

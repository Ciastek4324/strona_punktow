package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pl.punkty.app.model.MonthlySchedule;
import pl.punkty.app.model.MonthlyScheduleEntry;

import java.util.List;

public interface MonthlyScheduleEntryRepository extends JpaRepository<MonthlyScheduleEntry, Long> {
    List<MonthlyScheduleEntry> findAllByScheduleOrderBySlotCodeAscPositionAsc(MonthlySchedule schedule);
    void deleteBySchedule(MonthlySchedule schedule);
    @Modifying
    @Transactional
    @Query("delete from MonthlyScheduleEntry m where m.person.id = :personId")
    void deleteByPersonId(Long personId);
    long countBySchedule(MonthlySchedule schedule);
}

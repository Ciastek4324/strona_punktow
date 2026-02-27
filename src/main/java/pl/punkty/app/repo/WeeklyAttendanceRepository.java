package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;

import java.util.List;

public interface WeeklyAttendanceRepository extends JpaRepository<WeeklyAttendance, Long> {
    List<WeeklyAttendance> findByTableRef(WeeklyTable tableRef);
    List<WeeklyAttendance> findByTableRefIn(List<WeeklyTable> tableRefs);
    void deleteByTableRef(WeeklyTable tableRef);
    @Modifying
    @Transactional
    @Query("delete from WeeklyAttendance w where w.person.id = :personId")
    void deleteByPersonId(Long personId);
}

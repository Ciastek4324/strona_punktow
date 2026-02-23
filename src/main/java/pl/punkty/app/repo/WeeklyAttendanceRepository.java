package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;

import java.util.List;

public interface WeeklyAttendanceRepository extends JpaRepository<WeeklyAttendance, Long> {
    List<WeeklyAttendance> findByTableRef(WeeklyTable tableRef);
    List<WeeklyAttendance> findByTableRefIn(List<WeeklyTable> tableRefs);
    void deleteByTableRef(WeeklyTable tableRef);
}

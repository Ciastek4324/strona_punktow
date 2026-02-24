package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.MonthlySchedule;

import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyScheduleRepository extends JpaRepository<MonthlySchedule, Long> {
    Optional<MonthlySchedule> findByMonthDate(LocalDate monthDate);
}

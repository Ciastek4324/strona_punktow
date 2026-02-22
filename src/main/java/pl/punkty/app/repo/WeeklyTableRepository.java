package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.WeeklyTable;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyTableRepository extends JpaRepository<WeeklyTable, Long> {
    Optional<WeeklyTable> findByWeekStart(LocalDate weekStart);
}
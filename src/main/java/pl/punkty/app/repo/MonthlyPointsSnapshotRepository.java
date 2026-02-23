package pl.punkty.app.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.MonthlyPointsSnapshot;

import java.util.List;

public interface MonthlyPointsSnapshotRepository extends JpaRepository<MonthlyPointsSnapshot, Long> {
    List<MonthlyPointsSnapshot> findAllByOrderByCreatedAtDesc();
    Page<MonthlyPointsSnapshot> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

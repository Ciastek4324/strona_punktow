package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.PointsHistory;

import java.util.List;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {
    List<PointsHistory> findAllByOrderByChangedAtDesc();
}

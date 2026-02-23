package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.PointsSnapshot;

import java.util.Optional;

public interface PointsSnapshotRepository extends JpaRepository<PointsSnapshot, Long> {
    Optional<PointsSnapshot> findTopByOrderBySnapshotDateDesc();
}

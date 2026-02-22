package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.CurrentPoints;

public interface CurrentPointsRepository extends JpaRepository<CurrentPoints, Long> {
}
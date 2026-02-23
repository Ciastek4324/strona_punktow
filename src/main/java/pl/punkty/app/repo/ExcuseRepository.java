package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.Excuse;
import pl.punkty.app.model.ExcuseStatus;

import java.util.List;

public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    long countByStatus(ExcuseStatus status);
    List<Excuse> findAllByOrderByCreatedAtDesc();
}

package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.Excuse;

import java.util.List;

public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    long countByReadFlagFalse();
    List<Excuse> findAllByOrderByCreatedAtDesc();
}

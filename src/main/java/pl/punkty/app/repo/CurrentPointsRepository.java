package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pl.punkty.app.model.CurrentPoints;

public interface CurrentPointsRepository extends JpaRepository<CurrentPoints, Long> {
    @Modifying
    @Transactional
    @Query("delete from CurrentPoints c where c.person.id = :personId")
    void deleteByPersonId(Long personId);
}

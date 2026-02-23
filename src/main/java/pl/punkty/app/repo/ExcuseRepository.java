package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import pl.punkty.app.model.Excuse;
import pl.punkty.app.model.ExcuseStatus;

import java.util.List;

public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    long countByStatus(ExcuseStatus status);
    List<Excuse> findAllByOrderByCreatedAtDesc();
    Page<Excuse> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Excuse e set e.status = 'PENDING' where e.status is null")
    int backfillNullStatus();
}

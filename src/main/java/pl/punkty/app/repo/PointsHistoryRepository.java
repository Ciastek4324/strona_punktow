package pl.punkty.app.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import pl.punkty.app.model.PointsHistory;

import java.util.List;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {
    List<PointsHistory> findAllByOrderByChangedAtDesc();
    Page<PointsHistory> findAllByOrderByChangedAtDesc(Pageable pageable);
    Page<PointsHistory> findAllByPersonDisplayNameContainingIgnoreCaseOrderByChangedAtDesc(String query, Pageable pageable);
    @Modifying
    @Transactional
    @Query("delete from PointsHistory p where p.person.id = :personId")
    void deleteByPersonId(Long personId);
}

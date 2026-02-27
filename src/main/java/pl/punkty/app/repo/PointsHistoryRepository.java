package pl.punkty.app.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.PointsHistory;

import java.util.List;

public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {
    List<PointsHistory> findAllByOrderByChangedAtDesc();
    Page<PointsHistory> findAllByOrderByChangedAtDesc(Pageable pageable);
    Page<PointsHistory> findAllByPersonDisplayNameContainingIgnoreCaseOrderByChangedAtDesc(String query, Pageable pageable);
    void deleteByPersonId(Long personId);
}

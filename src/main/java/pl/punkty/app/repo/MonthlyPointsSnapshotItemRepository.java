package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.MonthlyPointsSnapshot;
import pl.punkty.app.model.MonthlyPointsSnapshotItem;

import java.util.List;

public interface MonthlyPointsSnapshotItemRepository extends JpaRepository<MonthlyPointsSnapshotItem, Long> {
    List<MonthlyPointsSnapshotItem> findAllBySnapshotOrderByPersonNameAsc(MonthlyPointsSnapshot snapshot);
}

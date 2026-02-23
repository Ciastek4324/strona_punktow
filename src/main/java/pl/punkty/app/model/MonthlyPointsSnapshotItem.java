package pl.punkty.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "monthly_points_snapshot_item")
public class MonthlyPointsSnapshotItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private MonthlyPointsSnapshot snapshot;

    @Column(nullable = false)
    private String personName;

    @Column(nullable = false)
    private int basePoints;

    @Column(nullable = false)
    private int monthPoints;

    @Column(nullable = false)
    private int totalPoints;

    public Long getId() {
        return id;
    }

    public MonthlyPointsSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(MonthlyPointsSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public int getBasePoints() {
        return basePoints;
    }

    public void setBasePoints(int basePoints) {
        this.basePoints = basePoints;
    }

    public int getMonthPoints() {
        return monthPoints;
    }

    public void setMonthPoints(int monthPoints) {
        this.monthPoints = monthPoints;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }
}

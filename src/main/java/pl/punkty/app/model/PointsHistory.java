package pl.punkty.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "points_history")
public class PointsHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Person person;

    @Column(nullable = false)
    private int oldPoints;

    @Column(nullable = false)
    private int newPoints;

    @Column(nullable = false)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public int getOldPoints() {
        return oldPoints;
    }

    public void setOldPoints(int oldPoints) {
        this.oldPoints = oldPoints;
    }

    public int getNewPoints() {
        return newPoints;
    }

    public void setNewPoints(int newPoints) {
        this.newPoints = newPoints;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}

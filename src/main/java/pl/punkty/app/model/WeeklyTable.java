package pl.punkty.app.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "weekly_tables")
public class WeeklyTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate weekStart;

    public Long getId() {
        return id;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }
}
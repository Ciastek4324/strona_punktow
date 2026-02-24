package pl.punkty.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "weekly_attendance")
public class WeeklyAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private WeeklyTable tableRef;

    @ManyToOne(optional = false)
    private Person person;

    @Column(nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private boolean present;

    @Column(nullable = false)
    private int otherCount = 0;

    public Long getId() {
        return id;
    }

    public WeeklyTable getTableRef() {
        return tableRef;
    }

    public void setTableRef(WeeklyTable tableRef) {
        this.tableRef = tableRef;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public int getOtherCount() {
        return otherCount;
    }

    public void setOtherCount(int otherCount) {
        this.otherCount = otherCount;
    }
}

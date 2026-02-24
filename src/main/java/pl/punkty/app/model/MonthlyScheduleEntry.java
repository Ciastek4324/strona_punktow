package pl.punkty.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "monthly_schedule_entry")
public class MonthlyScheduleEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private MonthlySchedule schedule;

    @ManyToOne(optional = false)
    private Person person;

    @Column(nullable = false)
    private int slotCode;

    @Column(nullable = false)
    private int position;

    public Long getId() {
        return id;
    }

    public MonthlySchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(MonthlySchedule schedule) {
        this.schedule = schedule;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public int getSlotCode() {
        return slotCode;
    }

    public void setSlotCode(int slotCode) {
        this.slotCode = slotCode;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}

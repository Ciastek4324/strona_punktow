package pl.punkty.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "people")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String displayName;

    @Enumerated(EnumType.STRING)
    private PersonRole role = PersonRole.MINISTRANT;

    private Integer basePoints = 0;

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public PersonRole getRole() {
        return role == null ? PersonRole.MINISTRANT : role;
    }

    public void setRole(PersonRole role) {
        this.role = role;
    }

    public int getBasePoints() {
        return basePoints == null ? 0 : basePoints;
    }

    public void setBasePoints(int basePoints) {
        this.basePoints = basePoints;
    }
}

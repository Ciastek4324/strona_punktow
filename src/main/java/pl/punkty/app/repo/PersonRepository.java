package pl.punkty.app.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.punkty.app.model.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
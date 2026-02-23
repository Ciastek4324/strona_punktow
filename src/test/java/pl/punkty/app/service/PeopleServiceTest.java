package pl.punkty.app.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.PersonRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PeopleServiceTest {
    @Test
    void doesNotAddDuplicate() {
        PersonRepository repo = Mockito.mock(PersonRepository.class);
        Mockito.when(repo.findByDisplayNameIgnoreCase("Jan Test"))
            .thenReturn(Optional.of(new Person()));
        PeopleService service = new PeopleService(repo);
        service.addPerson("Jan Test", PersonRole.MINISTRANT, 10);
        verify(repo, times(0)).save(any(Person.class));
    }

    @Test
    void clampsPointsOutOfRangeToZero() {
        PersonRepository repo = Mockito.mock(PersonRepository.class);
        PeopleService service = new PeopleService(repo);
        service.addPerson("Jan Test", PersonRole.MINISTRANT, 20000);
        verify(repo).save(Mockito.argThat(p -> p.getBasePoints() == 0));
    }

    @Test
    void trimsAndStoresName() {
        PersonRepository repo = Mockito.mock(PersonRepository.class);
        PeopleService service = new PeopleService(repo);
        service.addPerson("  Jan Test  ", PersonRole.MINISTRANT, 5);
        verify(repo).save(Mockito.argThat(p -> "Jan Test".equals(p.getDisplayName())));
    }
}

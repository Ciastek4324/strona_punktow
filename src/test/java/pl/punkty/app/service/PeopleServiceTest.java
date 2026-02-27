package pl.punkty.app.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.MonthlyScheduleEntryRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.PointsHistoryRepository;
import pl.punkty.app.repo.WeeklyAttendanceRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PeopleServiceTest {
    @Test
    void doesNotAddDuplicate() {
        PersonRepository repo = Mockito.mock(PersonRepository.class);
        Person existing = new Person();
        existing.setDisplayName("Jan Test");
        Mockito.when(repo.findAll()).thenReturn(List.of(existing));

        PeopleService service = newService(repo);
        service.addPerson("Jan Test", PersonRole.MINISTRANT, 10);
        verify(repo, times(0)).save(any(Person.class));
    }

    @Test
    void clampsPointsOutOfRangeToZero() {
        PersonRepository repo = Mockito.mock(PersonRepository.class);
        Mockito.when(repo.findAll()).thenReturn(List.of());
        PeopleService service = newService(repo);
        service.addPerson("Jan Test", PersonRole.MINISTRANT, 20000);
        verify(repo).save(Mockito.argThat(p -> p.getBasePoints() == 0));
    }

    @Test
    void trimsAndStoresName() {
        PersonRepository repo = Mockito.mock(PersonRepository.class);
        Mockito.when(repo.findAll()).thenReturn(List.of());
        PeopleService service = newService(repo);
        service.addPerson("  Jan Test  ", PersonRole.MINISTRANT, 5);
        verify(repo).save(Mockito.argThat(p -> "Jan Test".equals(p.getDisplayName())));
    }

    private static PeopleService newService(PersonRepository repo) {
        CurrentPointsRepository currentPointsRepository = Mockito.mock(CurrentPointsRepository.class);
        PointsHistoryRepository pointsHistoryRepository = Mockito.mock(PointsHistoryRepository.class);
        WeeklyAttendanceRepository weeklyAttendanceRepository = Mockito.mock(WeeklyAttendanceRepository.class);
        MonthlyScheduleEntryRepository monthlyScheduleEntryRepository = Mockito.mock(MonthlyScheduleEntryRepository.class);
        return new PeopleService(
            repo,
            currentPointsRepository,
            pointsHistoryRepository,
            weeklyAttendanceRepository,
            monthlyScheduleEntryRepository
        );
    }
}

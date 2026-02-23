package pl.punkty.app.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pl.punkty.app.model.Excuse;
import pl.punkty.app.model.ExcuseStatus;
import pl.punkty.app.repo.ExcuseRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExcuseServiceTest {
    @Test
    void rejectsInvalidDateRange() {
        ExcuseRepository repo = Mockito.mock(ExcuseRepository.class);
        ExcuseService service = new ExcuseService(repo);
        ExcuseService.Result result = service.submitExcuse(
            "Jan Test",
            LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 1),
            "Powod",
            "guest"
        );
        assertFalse(result.isOk());
        verify(repo, times(0)).save(any(Excuse.class));
    }

    @Test
    void acceptsValidExcuse() {
        ExcuseRepository repo = Mockito.mock(ExcuseRepository.class);
        ExcuseService service = new ExcuseService(repo);
        ExcuseService.Result result = service.submitExcuse(
            "Jan Test",
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 3),
            "Powod",
            "guest"
        );
        assertTrue(result.isOk());
        verify(repo, times(1)).save(any(Excuse.class));
    }

    @Test
    void setsPendingStatus() {
        ExcuseRepository repo = Mockito.mock(ExcuseRepository.class);
        ExcuseService service = new ExcuseService(repo);
        service.submitExcuse("Jan Test", null, null, "Powod", "guest");
        verify(repo).save(Mockito.argThat(e -> e.getStatus() == ExcuseStatus.PENDING && !e.isReadFlag()));
    }
}

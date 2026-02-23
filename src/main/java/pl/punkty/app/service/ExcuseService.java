package pl.punkty.app.service;

import org.springframework.stereotype.Service;
import pl.punkty.app.model.Excuse;
import pl.punkty.app.model.ExcuseStatus;
import pl.punkty.app.repo.ExcuseRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class ExcuseService {
    private final ExcuseRepository excuseRepository;

    public ExcuseService(ExcuseRepository excuseRepository) {
        this.excuseRepository = excuseRepository;
    }

    public Result submitExcuse(String fullName, LocalDate dateFrom, LocalDate dateTo, String reason, String createdBy) {
        String name = sanitizeName(fullName);
        String why = sanitizeReason(reason);
        if (name.isEmpty() || why.isEmpty()) {
            return Result.error("Brak wymaganych danych.");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            return Result.error("Niepoprawny zakres dat.");
        }
        Excuse excuse = new Excuse();
        excuse.setFullName(name);
        excuse.setDateFrom(dateFrom);
        excuse.setDateTo(dateTo);
        excuse.setReason(why);
        excuse.setCreatedBy(createdBy == null ? "guest" : createdBy);
        excuse.setStatus(ExcuseStatus.PENDING);
        excuse.setReadFlag(false);
        excuseRepository.save(excuse);
        return Result.ok();
    }

    public List<Excuse> listAll() {
        return excuseRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<Excuse> listPage(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        return excuseRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending())
        );
    }

    public long countPending() {
        return excuseRepository.countByStatus(ExcuseStatus.PENDING);
    }

    public void markAllRead(List<Excuse> items) {
        if (items.isEmpty()) {
            return;
        }
        for (Excuse excuse : items) {
            excuse.setReadFlag(true);
        }
        excuseRepository.saveAll(items);
    }

    public void approve(Long id, String reviewer) {
        excuseRepository.findById(id).ifPresent(excuse -> {
            excuse.setStatus(ExcuseStatus.APPROVED);
            excuse.setReviewedAt(LocalDateTime.now());
            excuse.setReviewedBy(reviewer);
            excuseRepository.save(excuse);
        });
    }

    public void reject(Long id, String reviewer) {
        excuseRepository.findById(id).ifPresent(excuse -> {
            excuse.setStatus(ExcuseStatus.REJECTED);
            excuse.setReviewedAt(LocalDateTime.now());
            excuse.setReviewedBy(reviewer);
            excuseRepository.save(excuse);
        });
    }

    private String sanitizeName(String value) {
        if (value == null) {
            return "";
        }
        String name = value.trim();
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }
        return name;
    }

    private String sanitizeReason(String value) {
        if (value == null) {
            return "";
        }
        String reason = value.trim();
        if (reason.length() > 1000) {
            reason = reason.substring(0, 1000);
        }
        return reason;
    }

    public static class Result {
        private final boolean ok;
        private final String error;

        private Result(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result error(String message) {
            return new Result(false, message);
        }

        public boolean isOk() {
            return ok;
        }

        public String getError() {
            return error;
        }
    }
}

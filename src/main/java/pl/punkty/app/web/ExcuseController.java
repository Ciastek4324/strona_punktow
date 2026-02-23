package pl.punkty.app.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.punkty.app.model.Excuse;
import pl.punkty.app.model.ExcuseStatus;
import pl.punkty.app.repo.ExcuseRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ExcuseController {
    private final ExcuseRepository excuseRepository;

    public ExcuseController(ExcuseRepository excuseRepository) {
        this.excuseRepository = excuseRepository;
    }

    @GetMapping("/excuses")
    public String excusesForm(Model model) {
        model.addAttribute("saved", false);
        return "excuses";
    }

    @PostMapping("/excuses/submit")
    public String submitExcuse(@RequestParam("fullName") String fullName,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                               @RequestParam("reason") String reason,
                               Model model) {
        String name = fullName.trim();
        String why = reason.trim();
        if (!name.isEmpty() && !why.isEmpty()) {
            Excuse excuse = new Excuse();
            excuse.setFullName(name);
            excuse.setDateFrom(dateFrom);
            excuse.setDateTo(dateTo);
            excuse.setReason(why);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String createdBy = (auth != null && auth.isAuthenticated()) ? auth.getName() : "guest";
            excuse.setCreatedBy(createdBy);
            excuseRepository.save(excuse);
        }
        model.addAttribute("saved", true);
        return "excuses";
    }

    @GetMapping("/excuses/inbox")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String excusesInbox(Model model) {
        List<Excuse> items = excuseRepository.findAllByOrderByCreatedAtDesc();
        long unread = excuseRepository.countByStatus(ExcuseStatus.PENDING);
        if (!items.isEmpty()) {
            for (Excuse excuse : items) {
                excuse.setReadFlag(true);
            }
            excuseRepository.saveAll(items);
        }
        model.addAttribute("items", items);
        model.addAttribute("unread", unread);
        return "excuses-inbox";
    }

    @PostMapping("/excuses/approve")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String approveExcuse(@RequestParam("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String reviewer = auth != null ? auth.getName() : "user";
        excuseRepository.findById(id).ifPresent(excuse -> {
            excuse.setStatus(ExcuseStatus.APPROVED);
            excuse.setReviewedAt(LocalDateTime.now());
            excuse.setReviewedBy(reviewer);
            excuseRepository.save(excuse);
        });
        return "redirect:/excuses/inbox";
    }

    @PostMapping("/excuses/reject")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String rejectExcuse(@RequestParam("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String reviewer = auth != null ? auth.getName() : "user";
        excuseRepository.findById(id).ifPresent(excuse -> {
            excuse.setStatus(ExcuseStatus.REJECTED);
            excuse.setReviewedAt(LocalDateTime.now());
            excuse.setReviewedBy(reviewer);
            excuseRepository.save(excuse);
        });
        return "redirect:/excuses/inbox";
    }
}

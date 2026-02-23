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
import pl.punkty.app.service.ExcuseService;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

@Controller
public class ExcuseController {
    private final ExcuseService excuseService;

    public ExcuseController(ExcuseService excuseService) {
        this.excuseService = excuseService;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = (auth != null && auth.isAuthenticated()) ? auth.getName() : "guest";
        ExcuseService.Result result = excuseService.submitExcuse(fullName, dateFrom, dateTo, reason, createdBy);
        model.addAttribute("saved", result.isOk());
        if (!result.isOk()) {
            model.addAttribute("error", result.getError());
        }
        return "excuses";
    }

    @GetMapping("/excuses/inbox")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String excusesInbox(@RequestParam(required = false, defaultValue = "0") int page,
                               Model model) {
        int safePage = Math.max(page, 0);
        Page<Excuse> itemsPage = excuseService.listPage(safePage, 50);
        List<Excuse> items = itemsPage.getContent();
        long unread = excuseService.countPending();
        excuseService.markAllRead(items);
        model.addAttribute("items", items);
        model.addAttribute("unread", unread);
        model.addAttribute("page", safePage);
        model.addAttribute("totalPages", itemsPage.getTotalPages());
        return "excuses-inbox";
    }

    @PostMapping("/excuses/approve")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String approveExcuse(@RequestParam("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String reviewer = auth != null ? auth.getName() : "user";
        excuseService.approve(id, reviewer);
        return "redirect:/excuses/inbox";
    }

    @PostMapping("/excuses/reject")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String rejectExcuse(@RequestParam("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String reviewer = auth != null ? auth.getName() : "user";
        excuseService.reject(id, reviewer);
        return "redirect:/excuses/inbox";
    }
}

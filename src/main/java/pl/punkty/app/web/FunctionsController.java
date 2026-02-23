package pl.punkty.app.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.service.PeopleService;

import java.util.List;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class FunctionsController {
    private final PeopleService peopleService;

    public FunctionsController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @GetMapping("/functions")
    public String functions(Model model) {
        List<Person> people = peopleService.getPeopleSorted();
        model.addAttribute("people", people);
        model.addAttribute("roles", PersonRole.values());
        return "functions";
    }

    @PostMapping("/functions/update")
    public String updateFunction(@RequestParam("personId") Long personId,
                                 @RequestParam("role") PersonRole role) {
        peopleService.updateRole(personId, role);
        return "redirect:/functions";
    }
}

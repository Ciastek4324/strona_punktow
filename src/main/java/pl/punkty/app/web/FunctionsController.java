package pl.punkty.app.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.repo.PersonRepository;

import java.util.List;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class FunctionsController {
    private final PersonRepository personRepository;

    public FunctionsController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @GetMapping("/functions")
    public String functions(Model model) {
        List<Person> people = personRepository.findAll().stream()
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();
        model.addAttribute("people", people);
        model.addAttribute("roles", PersonRole.values());
        return "functions";
    }

    @PostMapping("/functions/update")
    public String updateFunction(@RequestParam("personId") Long personId,
                                 @RequestParam("role") PersonRole role) {
        personRepository.findById(personId).ifPresent(person -> {
            person.setRole(role);
            personRepository.save(person);
        });
        return "redirect:/functions";
    }
}

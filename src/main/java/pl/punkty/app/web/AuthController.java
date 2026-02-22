package pl.punkty.app.web;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @PostMapping("/guest")
    public String loginAsGuest() {
        Authentication authRequest = new UsernamePasswordAuthenticationToken("guest", "");
        Authentication authResult = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authResult);
        return "redirect:/points/current";
    }
}
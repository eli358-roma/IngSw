package com.hackhub.controller;

import com.hackhub.model.User;
import com.hackhub.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    /**
     * Mostra la pagina di login
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session) {

        System.out.println("Tentativo login per email: " + email);

        //cerca l'utente per email
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            //per test: confronto semplice
            if ("password".equals(password)) {
                // Salva le informazioni utente in sessione
                session.setAttribute("userId", user.getId());
                session.setAttribute("userRole", user.getRole());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("userEmail", user.getEmail());

                System.out.println("Login riuscito: " + user.getUsername() + " (" + user.getRole() + ")");

                // Reindirizza in base al ruolo
                return switch (user.getRole()) {
                    case "ORGANIZER" -> "redirect:/dashboard";
                    case "JUDGE" -> "redirect:/judge/dashboard";
                    case "MENTOR" -> "redirect:/mentor/dashboard";
                    case "USER" -> "redirect:/teams";
                    default -> "redirect:/dashboard";
                };
            }
        }

        System.out.println("Login fallito per: " + email);
        // Login fallito
        return "redirect:/login?error=true";
    }

    /**
     * Logout
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/public";
    }
}
package com.hackhub.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        // Se l'utente è loggato, vai alla dashboard
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }

        // Altrimenti vai alla home pubblica
        return "redirect:/public";
    }
}

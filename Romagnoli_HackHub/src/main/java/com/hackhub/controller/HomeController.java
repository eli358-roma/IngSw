package com.hackhub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.stereotype.Controller;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Reindirizza alla dashboard
        return "redirect:/dashboard";
    }

}

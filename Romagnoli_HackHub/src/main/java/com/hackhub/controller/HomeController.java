package com.hackhub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("app", "HackHub");
        response.put("version", "1.0.0");
        response.put("description", "Piattaforma per la gestione di Hackathon");
        response.put("endpoints", Map.of(
                "users", "/api/users",
                "hackathons", "/api/hackathons",
                "teams", "/api/teams",
                "h2-console", "/h2-console"
        ));
        response.put("status", "running");
        return response;
    }
}
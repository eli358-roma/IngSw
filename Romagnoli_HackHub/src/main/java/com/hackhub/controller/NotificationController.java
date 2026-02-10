package com.hackhub.controller;

import com.hackhub.model.User;
import com.hackhub.pattern.strategy.NotificationContext;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationContext notificationContext;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type"); // "EMAIL" o "IN_APP"
        String message = (String) request.get("message");
        Long userId = Long.valueOf(request.get("userId").toString());

        // Recupera l'utente dal database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        notificationContext.sendNotification(type, message, user);

        return ResponseEntity.ok("Notifica inviata con successo");
    }
}
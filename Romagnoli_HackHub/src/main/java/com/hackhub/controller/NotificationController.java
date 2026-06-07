package com.hackhub.controller;

import com.hackhub.model.User;
import com.hackhub.service.NotificationService;
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
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        String message = (String) request.get("message");
        Long userId = Long.valueOf(request.get("userId").toString());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        notificationService.sendNotification(type, message, user);

        return ResponseEntity.ok("Notifica inviata con successo");
    }
}

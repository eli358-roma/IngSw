package com.hackhub.controller;

import com.hackhub.model.User;
import com.hackhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String username = request.get("username");
        String password = request.get("password");
        String role = request.get("role");

        User user = userService.createUser(email, username, password, role);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register/organizer")
    public ResponseEntity<User> registerOrganizer(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String username = request.get("username");
        String password = request.get("password");

        User user = userService.createOrganizer(email, username, password);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getUsersByRole("USER"));
    }

    @GetMapping("/judges")
    public ResponseEntity<List<User>> getJudges() {
        return ResponseEntity.ok(userService.getAvailableJudges());
    }

    @GetMapping("/mentors")
    public ResponseEntity<List<User>> getMentors() {
        return ResponseEntity.ok(userService.getAvailableMentors());
    }

   @PutMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newRole = request.get("role");
            User updatedUser = userService.updateUserRole(id, newRole);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ruolo aggiornato con successo");
            response.put("userId", updatedUser.getId());
            response.put("newRole", updatedUser.getRole());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Utente eliminato con successo"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}

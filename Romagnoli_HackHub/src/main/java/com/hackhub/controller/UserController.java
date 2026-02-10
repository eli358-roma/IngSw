package com.hackhub.controller;

import com.hackhub.model.User;
import com.hackhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<User> changeRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newRole = request.get("role");
        return ResponseEntity.ok(userService.updateUserRole(id, newRole));
    }
}
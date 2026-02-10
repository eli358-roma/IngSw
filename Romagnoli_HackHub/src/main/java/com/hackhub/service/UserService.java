package com.hackhub.service;

import com.hackhub.model.User;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Design Pattern: Factory Method
    public User createUser(String email, String username, String password, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email già registrata");
        }

        User user = new User(email, username, password, role);
        return userRepository.save(user);
    }

    public User createOrganizer(String email, String username, String password) {
        return createUser(email, username, password, "ORGANIZER");
    }

    public User createJudge(String email, String username, String password) {
        return createUser(email, username, password, "JUDGE");
    }

    public User createMentor(String email, String username, String password) {
        return createUser(email, username, password, "MENTOR");
    }

    public User createParticipant(String email, String username, String password) {
        return createUser(email, username, password, "USER");
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public List<User> getAvailableJudges() {
        return userRepository.findByRole("JUDGE");
    }

    public List<User> getAvailableMentors() {
        return userRepository.findByRole("MENTOR");
    }

    public User updateUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        user.setRole(newRole);
        return userRepository.save(user);
    }
}
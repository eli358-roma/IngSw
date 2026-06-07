package com.hackhub.service;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.repository.HackathonRepository;
import com.hackhub.repository.TeamRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servizio per la gestione degli utenti
 *
 * DESIGN PATTERN: FACTORY METHOD
 * I metodi createUser, createOrganizer, createJudge, createMentor, createParticipant
 * sono metodi factory che centralizzano la creazione di oggetti User,
 * nascondendo la complessità della validazione e della logica di creazione.
 */

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private HackathonRepository hackathonRepository;

    /**
     * Factory method base per creare utenti
     */
    public User createUser(String email, String username, String password, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email già registrata");
        }

        User user = new User(email, username, password, role);
        return userRepository.save(user);
    }

    //metodi factory specializzati
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

        List<String> validRoles = List.of("USER", "ORGANIZER", "JUDGE", "MENTOR");
        if (!validRoles.contains(newRole)) {
            throw new RuntimeException("Ruolo non valido: " + newRole);
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));

        //controlla se l'utente è creatore di qualche team
        List<Team> teamsWhereCreator = teamRepository.findByCreatorId(userId);
        if (!teamsWhereCreator.isEmpty()) {
            String teamNames = teamsWhereCreator.stream()
                    .map(Team::getName)
                    .collect(Collectors.joining(", "));
            throw new RuntimeException("Impossibile eliminare: l'utente è creatore dei seguenti team: " + teamNames +
                    ". Prima devi eliminare i team o trasferire la proprietà.");
        }

        //controlla se l'utente è giudice di qualche hackathon
        List<Hackathon> judgedHackathons = hackathonRepository.findByJudgeId(userId);
        if (!judgedHackathons.isEmpty()) {
            String hackathonNames = judgedHackathons.stream()
                    .map(Hackathon::getName)
                    .collect(Collectors.joining(", "));
            throw new RuntimeException("Impossibile eliminare: l'utente è giudice dei seguenti hackathon: " + hackathonNames +
                    ". Prima devi rimuoverlo come giudice.");
        }

        if (user.getTeam() != null) {
            Team team = user.getTeam();
            team.getMembers().remove(user);
            user.setTeam(null);
            teamRepository.save(team);
        }

        userRepository.delete(user);
        System.out.println("Utente eliminato con successo!");
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}

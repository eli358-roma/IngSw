package com.hackhub.service;

import com.hackhub.model.Hackathon;
import com.hackhub.model.User;
import com.hackhub.model.Team;
import com.hackhub.repository.HackathonRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.hackhub.pattern.observer.HackathonObservable;

@Service
public class HackathonService {

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackathonObservable hackathonObservable;

    // Design Pattern: Builder (semplificato)
    public Hackathon createHackathon(String name, String description, String rules,
                                     LocalDateTime regDeadline, LocalDateTime startDate,
                                     LocalDateTime endDate, Integer maxTeamSize, Long organizerId) {

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizzatore non trovato"));

        if (!"ORGANIZER".equals(organizer.getRole())) {
            throw new RuntimeException("L'utente non è un organizzatore");
        }

        Hackathon hackathon = new Hackathon(name, description, rules, regDeadline,
                startDate, endDate, maxTeamSize, organizer);

        return hackathonRepository.save(hackathon);
    }

    public Hackathon assignJudge(Long hackathonId, Long judgeId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new RuntimeException("Giudice non trovato"));

        if (!"JUDGE".equals(judge.getRole())) {
            throw new RuntimeException("L'utente non è un giudice");
        }

        hackathon.setJudge(judge);
        Hackathon saved = hackathonRepository.save(hackathon);

        // Notifica gli observer
        hackathonObservable.notifyJudgeAssigned(saved);

        return saved;
    }

    public Hackathon updateStatus(Long hackathonId, String newStatus) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        String oldStatus = hackathon.getStatus();

        if (!isValidStatus(newStatus)) {
            throw new RuntimeException("Stato non valido");
        }

        hackathon.setStatus(newStatus);
        Hackathon saved = hackathonRepository.save(hackathon);

        // Notifica gli observer
        hackathonObservable.notifyStatusChange(saved, oldStatus, newStatus);

        // Se concluso, determina vincitore
        if ("CONCLUSO".equals(newStatus)) {
            determineWinner(hackathon);
        }

        return saved;
    }

    private boolean isValidStatus(String status) {
        return List.of("INSCRIZIONE", "IN_CORSO", "IN_VALUTAZIONE", "CONCLUSO")
                .contains(status);
    }

    private void determineWinner(Hackathon hackathon) {
        List<Team> teams = hackathon.getTeams();

        if (teams.isEmpty()) {
            return;
        }

        // Trova il team con il punteggio più alto
        Team winner = null;
        Double maxScore = -1.0;

        for (Team team : teams) {
            if (team.getScore() != null && team.getScore() > maxScore) {
                maxScore = team.getScore();
                winner = team;
            }
        }

        if (winner != null) {
            hackathon.setWinnerTeamId(winner.getId());
            System.out.println("Vincitore: " + winner.getName() + " con punteggio: " + maxScore);
        }
    }

    public List<Hackathon> getAllHackathons() {
        return hackathonRepository.findAll();
    }

    public List<Hackathon> getHackathonsByStatus(String status) {
        return hackathonRepository.findByStatus(status);
    }

    public List<Hackathon> getHackathonsByOrganizer(Long organizerId) {
        return hackathonRepository.findByOrganizerId(organizerId);
    }

    public Hackathon declareWinner(Long hackathonId, Long teamId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        if (!"CONCLUSO".equals(hackathon.getStatus())) {
            throw new RuntimeException("L'hackathon non è concluso");
        }

        hackathon.setWinnerTeamId(teamId);
        Hackathon saved = hackathonRepository.save(hackathon);

        // Notifica gli observer
        hackathonObservable.notifyWinnerDeclared(saved, teamId);

        return saved;
    }

    public Hackathon addMentor(Long hackathonId, Long mentorId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor non trovato"));

        if (!"MENTOR".equals(mentor.getRole())) {
            throw new RuntimeException("L'utente non è un mentor");
        }

        if (!hackathon.getMentors().contains(mentor)) {
            hackathon.getMentors().add(mentor);
        }

        return hackathonRepository.save(hackathon);
    }

    public Hackathon removeMentor(Long hackathonId, Long mentorId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor non trovato"));

        hackathon.getMentors().remove(mentor);
        return hackathonRepository.save(hackathon);
    }

    public List<User> getMentors(Long hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));
        return hackathon.getMentors();
    }
}
package com.hackhub.service;

import com.hackhub.model.Hackathon;
import com.hackhub.model.User;
import com.hackhub.model.Team;
import com.hackhub.repository.HackathonRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

import com.hackhub.pattern.observer.HackathonObservable;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HackathonService {

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackathonObservable hackathonObservable;

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

    public Hackathon createHackathonWithBuilder(String name, String description, String rules, LocalDateTime regDeadline, LocalDateTime startDate, LocalDateTime endDate, Integer maxTeamSize, Long organizerId, Long judgeId, List<Long> mentorIds) {
        // Validazioni
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Il nome dell'hackathon è obbligatorio");
        }

        if (regDeadline.isAfter(startDate)) {
            throw new IllegalArgumentException("La scadenza iscrizioni deve essere prima dell'inizio");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La data di inizio deve essere prima della fine");
        }

        if (maxTeamSize < 1 || maxTeamSize > 10) {
            throw new IllegalArgumentException("La dimensione del team deve essere tra 1 e 10");
        }

        // Recupera organizzatore
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizzatore non trovato"));

        if (!"ORGANIZER".equals(organizer.getRole())) {
            throw new RuntimeException("L'utente non è un organizzatore");
        }

        // Crea hackathon base
        Hackathon hackathon = new Hackathon(name, description, rules, regDeadline,
                startDate, endDate, maxTeamSize, organizer);

        // Assegna giudice se presente
        if (judgeId != null) {
            User judge = userRepository.findById(judgeId)
                    .orElseThrow(() -> new RuntimeException("Giudice non trovato"));

            if (!"JUDGE".equals(judge.getRole())) {
                throw new RuntimeException("L'utente non è un giudice");
            }
            hackathon.setJudge(judge);
        }

        // Assegna mentori se presenti
        if (mentorIds != null && !mentorIds.isEmpty()) {
            for (Long mentorId : mentorIds) {
                User mentor = userRepository.findById(mentorId)
                        .orElseThrow(() -> new RuntimeException("Mentor non trovato"));

                if (!"MENTOR".equals(mentor.getRole())) {
                    throw new RuntimeException("L'utente " + mentor.getUsername() + " non è un mentor");
                }

                if (!hackathon.getMentors().contains(mentor)) {
                    hackathon.getMentors().add(mentor);
                }
            }
        }

        // Salva e notifica
        Hackathon saved = hackathonRepository.save(hackathon);

        // Notifica gli observer
        hackathonObservable.notifyJudgeAssigned(saved);

        System.out.println("Hackathon '" + name + "' creato con builder pattern");
        return saved;
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

    //Aggiorna automaticamente lo stato in base alle date
    @Scheduled(fixedRate = 3600000) // Ogni ora
    @Transactional
    public void updateHackathonStatuses() {
        List<Hackathon> hackathons = hackathonRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Hackathon hackathon : hackathons) {
            String oldStatus = hackathon.getStatus();
            String newStatus = null;

            switch (hackathon.getStatus()) {
                case "INSCRIZIONE":
                    if (now.isAfter(hackathon.getRegistrationDeadline())) {
                        newStatus = "IN_CORSO";
                    }
                    break;

                case "IN_CORSO":
                    if (now.isAfter(hackathon.getEndDate())) {
                        newStatus = "IN_VALUTAZIONE";
                    }
                    break;

                case "IN_VALUTAZIONE":
                    // Controlla se tutte le sottomissioni sono state valutate
                    boolean allEvaluated = true;
                    for (Team team : hackathon.getTeams()) {
                        if (team.hasSubmittedProject() && !team.isEvaluated()) {
                            allEvaluated = false;
                            break;
                        }
                    }

                    if (allEvaluated && !hackathon.getTeams().isEmpty()) {
                        newStatus = "CONCLUSO";
                        determineWinner(hackathon);
                    }
                    break;
            }

            if (newStatus != null && !newStatus.equals(oldStatus)) {
                hackathon.setStatus(newStatus);
                hackathonRepository.save(hackathon);
                hackathonObservable.notifyStatusChange(hackathon, oldStatus, newStatus);
                System.out.println("Hackathon '" + hackathon.getName() + "' passato a: " + newStatus);
            }
        }
    }

    /**
     * Ottiene statistiche complete per un hackathon
     */
    public Map<String, Object> getHackathonStatistics(Long hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        Map<String, Object> stats = new HashMap<>();

        // Statistiche base
        stats.put("name", hackathon.getName());
        stats.put("status", hackathon.getStatus());
        stats.put("totalTeams", hackathon.getTeams().size());

        // Statistiche team
        int teamsWithSubmission = 0;
        int teamsEvaluated = 0;
        double totalScore = 0;
        List<Double> scores = new ArrayList<>();

        for (Team team : hackathon.getTeams()) {
            if (team.hasSubmittedProject()) {
                teamsWithSubmission++;
            }
            if (team.isEvaluated()) {
                teamsEvaluated++;
                totalScore += team.getScore();
                scores.add(team.getScore());
            }
        }

        stats.put("teamsWithSubmission", teamsWithSubmission);
        stats.put("teamsEvaluated", teamsEvaluated);
        stats.put("avgScore", teamsEvaluated > 0 ? totalScore / teamsEvaluated : 0);
        stats.put("maxScore", scores.stream().max(Double::compare).orElse(0.0));
        stats.put("minScore", scores.stream().min(Double::compare).orElse(0.0));

        // Statistiche mentori
        stats.put("totalMentors", hackathon.getMentors().size());

        // Vincitore
        if (hackathon.getWinnerTeamId() != null) {
            Optional<Team> winner = hackathon.getTeams().stream()
                    .filter(t -> t.getId().equals(hackathon.getWinnerTeamId()))
                    .findFirst();

            winner.ifPresent(team -> {
                stats.put("winner", team.getName());
                stats.put("winnerScore", team.getScore());
            });
        }

        return stats;
    }

    //Verifica se un utente può accedere a un hackathon
    public boolean canAccessHackathon(Long hackathonId, Long userId, String userRole) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        // Organizzatore può sempre accedere
        if (hackathon.getOrganizer().getId().equals(userId)) {
            return true;
        }

        // Giudice può accedere se assegnato
        if ("JUDGE".equals(userRole) && hackathon.getJudge() != null
                && hackathon.getJudge().getId().equals(userId)) {
            return true;
        }

        // Mentore può accedere se nella lista
        if ("MENTOR".equals(userRole)) {
            return hackathon.getMentors().stream()
                    .anyMatch(m -> m.getId().equals(userId));
        }

        // Utente normale può accedere se partecipa
        if ("USER".equals(userRole)) {
            return hackathon.getTeams().stream()
                    .flatMap(t -> t.getMembers().stream())
                    .anyMatch(u -> u.getId().equals(userId));
        }

        return false;
    }

    public Hackathon getHackathonById(Long id) {
        return hackathonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato con ID: " + id));
    }
}

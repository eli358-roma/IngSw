package com.hackhub.service;

import com.hackhub.model.Hackathon;
import com.hackhub.model.User;
import com.hackhub.model.Team;
import com.hackhub.repository.HackathonRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class HackathonService {

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Crea un nuovo hackathon, metodo unificato con premio opzionale
     */
    public Hackathon createHackathon(String name, String description, String rules,
                                     LocalDateTime regDeadline, LocalDateTime startDate,
                                     LocalDateTime endDate, Integer maxTeamSize,
                                     Long organizerId, Double prizeMoney) {

        validateHackathonDates(regDeadline, startDate, endDate);
        validateMaxTeamSize(maxTeamSize);

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizzatore non trovato"));

        if (!"ORGANIZER".equals(organizer.getRole())) {
            throw new RuntimeException("Solo un organizzatore può creare hackathon");
        }

        Hackathon hackathon = new Hackathon(name, description, rules, regDeadline,
                startDate, endDate, maxTeamSize, organizer,
                prizeMoney != null ? prizeMoney : 0.0);

        return hackathonRepository.save(hackathon);
    }

    /**
     * Aggiorna lo stato e notifica i partecipanti
     */
    @Transactional
    public Hackathon updateStatus(Long hackathonId, String newStatus) {
        Hackathon hackathon = getHackathonById(hackathonId);

        if (!isValidStatus(newStatus)) {
            throw new RuntimeException("Stato non valido. Valori ammessi: " +
                    Arrays.toString(getValidStatuses()));
        }

        String oldStatus = hackathon.getStatus();
        hackathon.setStatus(newStatus);

        Hackathon saved = hackathonRepository.save(hackathon);

        notificationService.notifyHackathonStatusChange(saved, oldStatus, newStatus);

        // Se l'hackathon è concluso viene determinato e notificato il vincitore
        if ("CONCLUSO".equals(newStatus)) {
            determineAndNotifyWinner(hackathon);
        }

        return saved;
    }

    /**
     * Determina il vincitore in base al punteggio più alto
     */
    private void determineAndNotifyWinner(Hackathon hackathon) {
        if (hackathon.getTeams() == null || hackathon.getTeams().isEmpty()) {
            return;
        }

        Team winner = hackathon.getTeams().stream()
                .filter(Team::isEvaluated)
                .max(Comparator.comparing(Team::getScore))
                .orElse(null);

        if (winner != null) {
            hackathon.setWinnerTeamId(winner.getId());
            hackathonRepository.save(hackathon);

            //viene notificato il team vincitore
            notificationService.notifyWinner(winner, hackathon, hackathon.getPrizeMoney());
        }
    }

    /**
     * Assegna un giudice all'hackathon
     */
    @Transactional
    public Hackathon assignJudge(Long hackathonId, Long judgeId) {
        Hackathon hackathon = getHackathonById(hackathonId);
        User judge = userRepository.findById(judgeId)
                .orElseThrow(() -> new RuntimeException("Giudice non trovato"));

        if (!"JUDGE".equals(judge.getRole())) {
            throw new RuntimeException("L'utente non è un giudice");
        }

        hackathon.setJudge(judge);
        Hackathon saved = hackathonRepository.save(hackathon);

        //notifica il giudice
        notificationService.sendEmail(judge.getEmail(),
                "Sei stato assegnato come giudice",
                "Sei stato assegnato come giudice per l'hackathon: " + hackathon.getName());

        return saved;
    }

    private void validateHackathonDates(LocalDateTime regDeadline, LocalDateTime startDate, LocalDateTime endDate) {
        if (regDeadline == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Tutte le date sono obbligatorie");
        }
        if (regDeadline.isAfter(startDate)) {
            throw new IllegalArgumentException("La scadenza iscrizioni deve essere prima dell'inizio");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La data di inizio deve essere prima della fine");
        }
    }

    private void validateMaxTeamSize(Integer maxTeamSize) {
        if (maxTeamSize == null || maxTeamSize < 1 || maxTeamSize > 10) {
            throw new IllegalArgumentException("La dimensione del team deve essere tra 1 e 10");
        }
    }

    private boolean isValidStatus(String status) {
        return Arrays.asList("INSCRIZIONE", "IN_CORSO", "IN_VALUTAZIONE", "CONCLUSO").contains(status);
    }

    private String[] getValidStatuses() {
        return new String[]{"INSCRIZIONE", "IN_CORSO", "IN_VALUTAZIONE", "CONCLUSO"};
    }

    public Hackathon getHackathonById(Long id) {
        return hackathonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato con ID: " + id));
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

    /**
     * Aggiorna automaticamente lo stato in base alle date
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoUpdateStatuses() {
        List<Hackathon> hackathons = getAllHackathons();
        LocalDateTime now = LocalDateTime.now();

        for (Hackathon h : hackathons) {
            String newStatus = null;

            switch (h.getStatus()) {
                case "INSCRIZIONE":
                    if (now.isAfter(h.getRegistrationDeadline())) newStatus = "IN_CORSO";
                    break;
                case "IN_CORSO":
                    if (now.isAfter(h.getEndDate())) newStatus = "IN_VALUTAZIONE";
                    break;
                case "IN_VALUTAZIONE":
                    boolean allEvaluated = h.getTeams().stream()
                            .filter(Team::hasSubmittedProject)
                            .allMatch(Team::isEvaluated);
                    if (allEvaluated && !h.getTeams().isEmpty()) newStatus = "CONCLUSO";
                    break;
            }

            if (newStatus != null) {
                updateStatus(h.getId(), newStatus);
            }
        }
    }

    /**
     * Ottiene le statistiche complete per un hackathon
     */
    public Map<String, Object> getHackathonStatistics(Long hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        Map<String, Object> stats = new HashMap<>();

        stats.put("name", hackathon.getName());
        stats.put("status", hackathon.getStatus());
        stats.put("totalTeams", hackathon.getTeams().size());

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

        stats.put("totalMentors", hackathon.getMentors().size());

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

    /**
     * Verifica se un utente può accedere a un hackathon
     */
    public boolean canAccessHackathon(Long hackathonId, Long userId, String userRole) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        if (hackathon.getOrganizer().getId().equals(userId)) {
            return true;
        }

        if ("JUDGE".equals(userRole) && hackathon.getJudge() != null
                && hackathon.getJudge().getId().equals(userId)) {
            return true;
        }

        if ("MENTOR".equals(userRole)) {
            return hackathon.getMentors().stream()
                    .anyMatch(m -> m.getId().equals(userId));
        }

        if ("USER".equals(userRole)) {
            return hackathon.getTeams().stream()
                    .flatMap(t -> t.getMembers().stream())
                    .anyMatch(u -> u.getId().equals(userId));
        }

        return false;
    }
}

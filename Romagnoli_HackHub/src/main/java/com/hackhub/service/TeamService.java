package com.hackhub.service;


import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.model.Hackathon;
import com.hackhub.repository.TeamRepository;
import com.hackhub.repository.UserRepository;
import com.hackhub.repository.HackathonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Crea un nuovo team
     */
    public Team createTeam(String teamName, Long hackathonId, Long creatorId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        if (!hackathon.isRegistrationOpen()) {
            throw new RuntimeException("Le iscrizioni per questo hackathon sono chiuse");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (creator.getTeam() != null) {
            throw new RuntimeException("Questo utente è già in un team: " + creator.getTeam().getName());
        }

        Team team = new Team(teamName, hackathon, creator);
        return teamRepository.save(team);
    }

    /**
     * Aggiunge un utente al team
     */
    public void joinTeam(Long teamId, Long userId) {
        Team team = getTeamById(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (user.getTeam() != null) {
            throw new RuntimeException("Utente già in un team: " + user.getTeam().getName());
        }
        if (team.isFull()) {
            throw new RuntimeException("Team al completo (max " + team.getHackathon().getMaxTeamSize() + " membri)");
        }

        user.joinTeam(team);
        userRepository.save(user);
        teamRepository.save(team);

        notificationService.sendInApp(userId, "Sei entrato nel team " + team.getName());
    }

    /**
     * Valutazione team
     */
    public void evaluateTeam(Long teamId, Double score, String feedback, Long judgeId) {
        Team team = getTeamById(teamId);
        Hackathon hackathon = team.getHackathon();

        if (!team.hasSubmittedProject()) {
            throw new RuntimeException("Il team non ha ancora inviato un progetto");
        }
        if (team.isEvaluated()) {
            throw new RuntimeException("Questo team è già stato valutato con punteggio: " + team.getScore());
        }
        if (!"IN_VALUTAZIONE".equals(hackathon.getStatus())) {
            throw new RuntimeException("L'hackathon non è in fase di valutazione");
        }
        if (hackathon.getJudge() == null || !hackathon.getJudge().getId().equals(judgeId)) {
            throw new RuntimeException("Non sei il giudice assegnato a questo hackathon");
        }
        if (score < 0 || score > 10) {
            throw new RuntimeException("Il punteggio deve essere compreso tra 0 e 10");
        }

        team.evaluate(score, feedback);
        teamRepository.save(team);

        //notifica il team valutato
        notificationService.notifyTeamEvaluated(team, score, feedback);

        //verifica che tutti i team siano stati valutati
        boolean allEvaluated = hackathon.getTeams().stream()
                .filter(Team::hasSubmittedProject)
                .allMatch(Team::isEvaluated);

        if (allEvaluated && "IN_VALUTAZIONE".equals(hackathon.getStatus())) {
            System.out.println("Tutti i team valutati per l'hackathon: " + hackathon.getName());
        }
    }

    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team non trovato con ID: " + id));
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    public List<Team> getTeamsByHackathon(Long hackathonId) {
        return teamRepository.findByHackathonId(hackathonId);
    }

    public List<Team> findTeamsByMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (user.getTeam() != null) {
            return List.of(user.getTeam());
        }
        return List.of();
    }

    /**
     * Ottiene tutti gli utenti disponibili per unirsi a un team
     */
    public List<User> getAvailableUsersForTeam(Long teamId) {
        Team team = getTeamById(teamId);

        return userRepository.findByRole("USER").stream()
                .filter(user -> user.getTeam() == null && !team.hasMember(user))
                .collect(Collectors.toList());
    }

    /**
     * Rimuove un utente dal team
     */
    public void leaveTeam(Long teamId, Long userId) {
        Team team = getTeamById(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (!team.hasMember(user)) {
            throw new RuntimeException("L'utente non fa parte di questo team");
        }

        if (team.isCreator(user)) {
            throw new RuntimeException("Il creatore del team non può uscire. Trasferisci la leadership prima di uscire.");
        }
        user.leaveTeam();
        userRepository.save(user);
        teamRepository.save(team);

        notificationService.sendInApp(userId, "Hai lasciato il team " + team.getName());
    }

    /**
     * Sottomissione progetto
     */
    public void submitProject(Long teamId, String projectName, String description, String repoUrl) {
        Team team = getTeamById(teamId);
        Hackathon hackathon = team.getHackathon();

        if (!hackathon.isInProgress()) {
            throw new RuntimeException("L'hackathon non è in corso. Stato attuale: " + hackathon.getStatus());
        }
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new RuntimeException("Il nome del progetto è obbligatorio");
        }
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            throw new RuntimeException("L'URL del repository è obbligatorio");
        }

        team.submitProject(projectName, description, repoUrl);
        teamRepository.save(team);

        String message = "Il team ha inviato il progetto: " + projectName;
        for (User member : team.getMembers()) {
            notificationService.sendInApp(member.getId(), message);
        }
    }

    /**
     * Elimina un team
     */
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        //rimuovi tutti i membri dal team prima di eliminarlo
        for (User member : team.getMembers()) {
            member.leaveTeam();
            userRepository.save(member);
        }

        teamRepository.delete(team);
        System.out.println("Team " + team.getName() + " eliminato");
    }

    /**
     * Verifica se un utente può unirsi a un team
     */
    public boolean canJoinTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato con ID: " + teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));

        if (user.getTeam() != null) {
            return false;
        }

        if (team.isFull()) {
            return false;
        }

        if (!team.getHackathon().isRegistrationOpen()) {
            return false;
        }

        if (team.hasMember(user)) {
            return false;
        }

        return true;
    }

    /**
     * Ottiene statistiche sui team di un hackathon
     */
    public TeamStatistics getTeamStatistics(Long hackathonId) {
        List<Team> teams = teamRepository.findByHackathonId(hackathonId);

        int totalTeams = teams.size();
        int teamsWithSubmission = 0;
        int teamsEvaluated = 0;
        double totalScore = 0.0;
        double maxScore = 0.0;
        double minScore = 10.0;

        for (Team team : teams) {
            if (team.hasSubmittedProject()) {
                teamsWithSubmission++;
            }

            if (team.isEvaluated()) {
                teamsEvaluated++;
                double score = team.getScore();
                totalScore += score;

                if (score > maxScore) {
                    maxScore = score;
                }

                if (score < minScore) {
                    minScore = score;
                }
            }
        }

        double avgScore = teamsEvaluated > 0 ? totalScore / teamsEvaluated : 0.0;

        if (teamsEvaluated == 0) {
            minScore = 0.0;
        }

        return new TeamStatistics(totalTeams, teamsWithSubmission, teamsEvaluated,
                avgScore, maxScore, minScore);
    }

    /**
     * Classe interna per le statistiche dei team
     */
    public static class TeamStatistics {
        private final int totalTeams;
        private final int teamsWithSubmission;
        private final int teamsEvaluated;
        private final double averageScore;
        private final double maxScore;
        private final double minScore;

        public TeamStatistics(int totalTeams, int teamsWithSubmission,
                              int teamsEvaluated, double averageScore,
                              double maxScore, double minScore) {
            this.totalTeams = totalTeams;
            this.teamsWithSubmission = teamsWithSubmission;
            this.teamsEvaluated = teamsEvaluated;
            this.averageScore = averageScore;
            this.maxScore = maxScore;
            this.minScore = minScore;
        }

        public int getTotalTeams() { return totalTeams; }
        public int getTeamsWithSubmission() { return teamsWithSubmission; }
        public int getTeamsEvaluated() { return teamsEvaluated; }
        public double getAverageScore() { return averageScore; }
        public double getMaxScore() { return maxScore; }
        public double getMinScore() { return minScore; }

        public double getSubmissionRate() {
            return totalTeams > 0 ?
                    (double) teamsWithSubmission / totalTeams * 100 : 0;
        }

        public double getEvaluationRate() {
            return totalTeams > 0 ?
                    (double) teamsEvaluated / totalTeams * 100 : 0;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("totalTeams", totalTeams);
            map.put("teamsWithSubmission", teamsWithSubmission);
            map.put("teamsEvaluated", teamsEvaluated);
            map.put("averageScore", Math.round(averageScore * 100) / 100.0);
            map.put("maxScore", maxScore);
            map.put("minScore", minScore);
            map.put("submissionRate", Math.round(getSubmissionRate() * 100) / 100.0);
            map.put("evaluationRate", Math.round(getEvaluationRate() * 100) / 100.0);
            return map;
        }

        @Override
        public String toString() {
            return String.format(
                    "TeamStatistics[total=%d, submissions=%d, evaluated=%d, avgScore=%.2f]",
                    totalTeams, teamsWithSubmission, teamsEvaluated, averageScore
            );
        }
    }
}

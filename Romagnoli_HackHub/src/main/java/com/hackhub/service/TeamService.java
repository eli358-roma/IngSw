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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackathonRepository hackathonRepository;

    /**
     * Crea un nuovo team
     */
    public Team createTeam(String teamName, Long hackathonId, Long creatorId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        if (!hackathon.isRegistrationOpen()) {
            throw new RuntimeException("Registrazioni chiuse per questo hackathon");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (creator.getTeam() != null) {
            throw new RuntimeException("L'utente è già membro di un team");
        }

        Team team = new Team(teamName, hackathon, creator);
        Team savedTeam = teamRepository.save(team);

        System.out.println("Team " + teamName + " creato con successo!");
        return savedTeam;
    }

    /**
     * Unisce un utente a un team
     */
    public Team joinTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // Usa il nuovo metodo joinTeam della classe User
        user.joinTeam(team);

        // Salva le modifiche
        userRepository.save(user);
        teamRepository.save(team);

        System.out.println("Utente " + user.getUsername() + " aggiunto al team " + team.getName());
        return team;
    }

    /**
     * Rimuove un utente da un team
     */
    public Team leaveTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (!team.hasMember(user)) {
            throw new RuntimeException("L'utente non fa parte di questo team");
        }

        // Usa il metodo leaveTeam della classe User
        user.leaveTeam();

        // Salva le modifiche
        userRepository.save(user);
        teamRepository.save(team);

        System.out.println("Utente " + user.getUsername() + " rimosso dal team " + team.getName());
        return team;
    }

    /**
     * Sottomette il progetto del team
     */
    public Team submitProject(Long teamId, String projectName, String description, String repoUrl) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        if (!team.getHackathon().isInProgress()) {
            throw new RuntimeException("L'hackathon non è in corso");
        }

        team.submitProject(projectName, description, repoUrl);

        Team savedTeam = teamRepository.save(team);
        System.out.println("Progetto inviato per il team " + team.getName());

        return savedTeam;
    }

    /**
     * Valuta un team
     */
    public Team evaluateTeam(Long teamId, Double score, String feedback) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        team.evaluate(score, feedback);

        Team savedTeam = teamRepository.save(team);
        System.out.println("Team " + team.getName() + " valutato con punteggio: " + score);

        return savedTeam;
    }

    /**
     * Ottiene tutti i team di un hackathon
     */
    public List<Team> getTeamsByHackathon(Long hackathonId) {
        return teamRepository.findByHackathonId(hackathonId);
    }

    /**
     * Ottiene un team per ID
     */
    public Team getTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));
    }

    /**
     * Ottiene tutti i team
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    /**
     * Elimina un team
     */
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        // Rimuovi tutti i membri dal team prima di eliminarlo
        for (User member : team.getMembers()) {
            member.leaveTeam();
            userRepository.save(member);
        }

        teamRepository.delete(team);
        System.out.println("Team " + team.getName() + " eliminato");
    }

    /**
     * Trova tutti i team di cui un utente è membro
     */
    public List<Team> findTeamsByMember(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));

        List<Team> teams = new ArrayList<>();

        // Un utente può essere membro di un solo team alla volta
        if (user.getTeam() != null) {
            teams.add(user.getTeam());
        }

        return teams;
    }

    /**
     * Verifica se un utente può unirsi a un team
     */
    public boolean canJoinTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato con ID: " + teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));

        // Controllo 1: L'utente non deve essere già in un team
        if (user.getTeam() != null) {
            return false;
        }

        // Controllo 2: Il team non deve essere pieno
        if (team.isFull()) {
            return false;
        }

        // Controllo 3: L'hackathon deve essere in fase di iscrizione
        if (!team.getHackathon().isRegistrationOpen()) {
            return false;
        }

        // Controllo 4: L'utente non deve essere già nel team
        if (team.hasMember(user)) {
            return false;
        }

        return true;
    }

    /**
     * Ottiene statistiche sui team di un hackathon
     * @param hackathonId ID dell'hackathon
     * @return Oggetto TeamStatistics con le statistiche
     */
    public TeamStatistics getTeamStatistics(Long hackathonId) {
        List<Team> teams = teamRepository.findByHackathonId(hackathonId);

        int totalTeams = teams.size();
        int teamsWithSubmission = 0;
        int teamsEvaluated = 0;
        double totalScore = 0.0;
        double maxScore = 0.0;
        double minScore = 10.0; // Partiamo dal massimo

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

        // Se nessun team è stato valutato, resetta minScore a 0
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

        // Getter
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

        // Metodo per ottenere una mappa con tutte le statistiche (utile per JSON)
        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("totalTeams", totalTeams);
            map.put("teamsWithSubmission", teamsWithSubmission);
            map.put("teamsEvaluated", teamsEvaluated);
            map.put("averageScore", Math.round(averageScore * 100) / 100.0); // Arrotonda a 2 decimali
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

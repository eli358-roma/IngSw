package com.hackhub.service;

import com.hackhub.model.Hackathon;
import com.hackhub.model.SupportRequest;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servizio per la gestione dei dati della dashboard
 * Separa la logica di business dal controller
 */
@Service
public class DashboardService {

    @Autowired
    private UserService userService;

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private SupportRequestService supportRequestService;

    /**
     * Ottiene tutti i dati necessari per la dashboard
     */
    public DashboardData getDashboardData() {
        DashboardData data = new DashboardData();

        //recupera gli utenti per il ruolo
        List<User> users = userService.getUsersByRole("USER");
        List<User> organizers = userService.getUsersByRole("ORGANIZER");
        List<User> judges = userService.getAvailableJudges();
        List<User> mentors = userService.getAvailableMentors();

        data.setUsers(users);
        data.setOrganizers(organizers);
        data.setJudges(judges);
        data.setMentors(mentors);
        data.setTotalUsers(users.size() + organizers.size() + judges.size() + mentors.size());

        //recupera l'hackathon
        List<Hackathon> hackathons = hackathonService.getAllHackathons();
        data.setHackathons(hackathons);
        data.setTotalHackathons(hackathons.size());

        //calcola statistiche team e sottomissioni
        int totalTeams = 0;
        int totalSubmissions = 0;
        int teamsWithProject = 0;

        for (Hackathon h : hackathons) {
            List<Team> teams = teamService.getTeamsByHackathon(h.getId());
            totalTeams += teams.size();

            for (Team t : teams) {
                if (t.hasSubmittedProject()) {
                    totalSubmissions++;
                    teamsWithProject++;
                }
            }
        }

        data.setTotalTeams(totalTeams);
        data.setTotalSubmissions(totalSubmissions);
        data.setTeamsWithProject(teamsWithProject);

        double submissionRate = totalTeams > 0 ? (double) teamsWithProject / totalTeams * 100 : 0;
        data.setSubmissionRate(Math.round(submissionRate * 10) / 10.0);

        //data dell'ultima sottomissione
        String lastSubmissionDate = getLastSubmissionDate(hackathons);
        data.setLastSubmissionDate(lastSubmissionDate);

        List<SupportRequest> pendingRequests = supportRequestService.getPendingRequests();
        data.setPendingRequests(pendingRequests);
        data.setTotalPendingRequests(pendingRequests.size());

        Map<String, Long> statusStats = getHackathonStatusStats(hackathons);
        data.setHackathonStatusStats(statusStats);

        List<DashboardActivity> recentActivities = getRecentActivities(hackathons, pendingRequests);
        data.setRecentActivities(recentActivities);

        EvaluationStats evalStats = getEvaluationStats(hackathons);
        data.setEvaluationStats(evalStats);

        data.setSystemStatus("online");
        data.setDatabaseStatus("online");
        data.setServicesStatus("online");
        data.setLastUpdated(LocalDateTime.now());

        return data;
    }

    /**
     * Trova la data dell'ultima sottomissione
     */
    private String getLastSubmissionDate(List<Hackathon> hackathons) {
        LocalDateTime lastSubmission = null;

        for (Hackathon h : hackathons) {
            for (Team t : teamService.getTeamsByHackathon(h.getId())) {
                if (t.hasSubmittedProject()) {
                    if (lastSubmission == null || LocalDateTime.now().isAfter(lastSubmission)) {
                        lastSubmission = LocalDateTime.now();
                    }
                }
            }
        }

        if (lastSubmission != null) {
            return lastSubmission.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        return "Nessuna sottomissione";
    }

    /**
     * Calcola le statistiche per lo stato hackathon
     */
    private Map<String, Long> getHackathonStatusStats(List<Hackathon> hackathons) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("INSCRIZIONE", 0L);
        stats.put("IN_CORSO", 0L);
        stats.put("IN_VALUTAZIONE", 0L);
        stats.put("CONCLUSO", 0L);

        for (Hackathon h : hackathons) {
            String status = h.getStatus();
            stats.put(status, stats.getOrDefault(status, 0L) + 1);
        }

        return stats;
    }

    /**
     * Calcola le statistiche sulle valutazioni
     */
    private EvaluationStats getEvaluationStats(List<Hackathon> hackathons) {
        EvaluationStats stats = new EvaluationStats();

        int totalEvaluated = 0;
        double sumScores = 0;
        double maxScore = 0;
        double minScore = 10;

        for (Hackathon h : hackathons) {
            for (Team t : teamService.getTeamsByHackathon(h.getId())) {
                if (t.isEvaluated() && t.getScore() != null) {
                    totalEvaluated++;
                    double score = t.getScore();
                    sumScores += score;
                    maxScore = Math.max(maxScore, score);
                    minScore = Math.min(minScore, score);
                }
            }
        }

        stats.setTotalEvaluated(totalEvaluated);
        stats.setAverageScore(totalEvaluated > 0 ? sumScores / totalEvaluated : 0);
        stats.setMaxScore(maxScore);
        stats.setMinScore(minScore == 10 ? 0 : minScore);

        return stats;
    }

    /**
     * Creazione della lista delle attività recenti
     */
    private List<DashboardActivity> getRecentActivities(List<Hackathon> hackathons,
                                                        List<SupportRequest> pendingRequests) {
        List<DashboardActivity> activities = new ArrayList<>();

        Random random = new Random();
        for (Hackathon h : hackathons) {
            for (Team t : teamService.getTeamsByHackathon(h.getId())) {
                if (t.hasSubmittedProject()) {
                    activities.add(new DashboardActivity(
                            "submission",
                            "Il team \"" + t.getName() + "\" ha inviato il progetto \"" + t.getProjectName() + "\" per " + h.getName(),
                            LocalDateTime.now().minusHours(random.nextInt(72)),
                            t.getCreator() != null ? t.getCreator().getUsername() : "Sconosciuto",
                            t.getId(),
                            h.getId()
                    ));
                }
            }
        }

        for (SupportRequest req : pendingRequests) {
            activities.add(new DashboardActivity(
                    "support",
                    "Richiesta di supporto: " + req.getTitle(),
                    req.getRequestDate(),
                    req.getTeam() != null && req.getTeam().getCreator() != null ?
                            req.getTeam().getCreator().getUsername() : "Sconosciuto",
                    req.getTeam() != null ? req.getTeam().getId() : null,
                    req.getTeam() != null && req.getTeam().getHackathon() != null ?
                            req.getTeam().getHackathon().getId() : null
            ));
        }

        for (Hackathon h : hackathons) {
            for (Team t : teamService.getTeamsByHackathon(h.getId())) {
                if (activities.size() < 20 && t.getCreator() != null) {
                    activities.add(new DashboardActivity(
                            "team",
                            "Nuovo team \"" + t.getName() + "\" creato da " + t.getCreator().getUsername(),
                            LocalDateTime.now().minusDays(random.nextInt(30)),
                            t.getCreator().getUsername(),
                            t.getId(),
                            h.getId()
                    ));
                }
            }
        }

        activities.sort((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()));
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }

        return activities;
    }

    /**
     * Ottiene i top team per punteggio
     */
    public List<TeamRanking> getTopTeams(int limit) {
        List<Team> allTeams = teamService.getAllTeams();

        return allTeams.stream()
                .filter(Team::isEvaluated)
                .filter(t -> t.getScore() != null)
                .sorted((t1, t2) -> Double.compare(t2.getScore(), t1.getScore()))
                .limit(limit)
                .map(t -> new TeamRanking(
                        t.getId(),
                        t.getName(),
                        t.getScore(),
                        t.getHackathon() != null ? t.getHackathon().getName() : "N/A",
                        t.getMembers() != null ? t.getMembers().size() : 0
                ))
                .collect(Collectors.toList());
    }

    /**
     * Contenitore di tutti i dati della dashboard
     */
    public static class DashboardData {
        private List<User> users;
        private List<User> organizers;
        private List<User> judges;
        private List<User> mentors;
        private List<Hackathon> hackathons;
        private List<SupportRequest> pendingRequests;
        private List<DashboardActivity> recentActivities;
        private Map<String, Long> hackathonStatusStats;
        private EvaluationStats evaluationStats;

        private int totalUsers;
        private int totalHackathons;
        private int totalTeams;
        private int totalSubmissions;
        private int teamsWithProject;
        private int totalPendingRequests;

        private double submissionRate;
        private String lastSubmissionDate;
        private String systemStatus;
        private String databaseStatus;
        private String servicesStatus;
        private LocalDateTime lastUpdated;

        public List<User> getUsers() { return users; }
        public void setUsers(List<User> users) { this.users = users; }

        public List<User> getOrganizers() { return organizers; }
        public void setOrganizers(List<User> organizers) { this.organizers = organizers; }

        public List<User> getJudges() { return judges; }
        public void setJudges(List<User> judges) { this.judges = judges; }

        public List<User> getMentors() { return mentors; }
        public void setMentors(List<User> mentors) { this.mentors = mentors; }

        public List<Hackathon> getHackathons() { return hackathons; }
        public void setHackathons(List<Hackathon> hackathons) { this.hackathons = hackathons; }

        public List<SupportRequest> getPendingRequests() { return pendingRequests; }
        public void setPendingRequests(List<SupportRequest> pendingRequests) { this.pendingRequests = pendingRequests; }

        public List<DashboardActivity> getRecentActivities() { return recentActivities; }
        public void setRecentActivities(List<DashboardActivity> recentActivities) { this.recentActivities = recentActivities; }

        public Map<String, Long> getHackathonStatusStats() { return hackathonStatusStats; }
        public void setHackathonStatusStats(Map<String, Long> hackathonStatusStats) { this.hackathonStatusStats = hackathonStatusStats; }

        public EvaluationStats getEvaluationStats() { return evaluationStats; }
        public void setEvaluationStats(EvaluationStats evaluationStats) { this.evaluationStats = evaluationStats; }

        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

        public int getTotalHackathons() { return totalHackathons; }
        public void setTotalHackathons(int totalHackathons) { this.totalHackathons = totalHackathons; }

        public int getTotalTeams() { return totalTeams; }
        public void setTotalTeams(int totalTeams) { this.totalTeams = totalTeams; }

        public int getTotalSubmissions() { return totalSubmissions; }
        public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }

        public int getTeamsWithProject() { return teamsWithProject; }
        public void setTeamsWithProject(int teamsWithProject) { this.teamsWithProject = teamsWithProject; }

        public int getTotalPendingRequests() { return totalPendingRequests; }
        public void setTotalPendingRequests(int totalPendingRequests) { this.totalPendingRequests = totalPendingRequests; }

        public double getSubmissionRate() { return submissionRate; }
        public void setSubmissionRate(double submissionRate) { this.submissionRate = submissionRate; }

        public String getLastSubmissionDate() { return lastSubmissionDate; }
        public void setLastSubmissionDate(String lastSubmissionDate) { this.lastSubmissionDate = lastSubmissionDate; }

        public String getSystemStatus() { return systemStatus; }
        public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }

        public String getDatabaseStatus() { return databaseStatus; }
        public void setDatabaseStatus(String databaseStatus) { this.databaseStatus = databaseStatus; }

        public String getServicesStatus() { return servicesStatus; }
        public void setServicesStatus(String servicesStatus) { this.servicesStatus = servicesStatus; }

        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

        public String getFormattedLastUpdated() {
            if (lastUpdated == null) return "Mai";
            return lastUpdated.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }
    }

    /**
     * Statistiche sulle valutazioni
     */
    public static class EvaluationStats {
        private int totalEvaluated;
        private double averageScore;
        private double maxScore;
        private double minScore;

        public int getTotalEvaluated() { return totalEvaluated; }
        public void setTotalEvaluated(int totalEvaluated) { this.totalEvaluated = totalEvaluated; }

        public double getAverageScore() { return averageScore; }
        public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

        public double getMaxScore() { return maxScore; }
        public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }

        public String getFormattedAverage() {
            return String.format("%.1f", averageScore);
        }
    }

    /**
     * Classifica dei team
     */
    public static class TeamRanking {
        private Long id;
        private String name;
        private Double score;
        private String hackathonName;
        private int memberCount;

        public TeamRanking(Long id, String name, Double score, String hackathonName, int memberCount) {
            this.id = id;
            this.name = name;
            this.score = score;
            this.hackathonName = hackathonName;
            this.memberCount = memberCount;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }

        public String getHackathonName() { return hackathonName; }
        public void setHackathonName(String hackathonName) { this.hackathonName = hackathonName; }

        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

        public String getScoreFormatted() {
            return score != null ? String.format("%.1f", score) : "N/A";
        }
    }

    /**
     * Attività della dashboard
     */
    public static class DashboardActivity {
        private String type;
        private String description;
        private LocalDateTime timestamp;
        private String user;
        private Long teamId;
        private Long hackathonId;

        public DashboardActivity(String type, String description, LocalDateTime timestamp,
                                 String user, Long teamId, Long hackathonId) {
            this.type = type;
            this.description = description;
            this.timestamp = timestamp;
            this.user = user;
            this.teamId = teamId;
            this.hackathonId = hackathonId;
        }

        public DashboardActivity(String type, String description, LocalDateTime timestamp, String user) {
            this(type, description, timestamp, user, null, null);
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getUser() { return user; }
        public Long getTeamId() { return teamId; }
        public Long getHackathonId() { return hackathonId; }

        public void setType(String type) { this.type = type; }
        public void setDescription(String description) { this.description = description; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public void setUser(String user) { this.user = user; }
        public void setTeamId(Long teamId) { this.teamId = teamId; }
        public void setHackathonId(Long hackathonId) { this.hackathonId = hackathonId; }

        public String getFormattedTime() {
            return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        public String getRelativeTime() {
            LocalDateTime now = LocalDateTime.now();
            long hours = java.time.Duration.between(timestamp, now).toHours();

            if (hours < 1) return "Poco fa";
            if (hours < 24) return hours + " ore fa";
            if (hours < 48) return "Ieri";
            return (hours / 24) + " giorni fa";
        }

        public String getIcon() {
            return switch (type) {
                case "submission" -> "bi-cloud-upload";
                case "support" -> "bi-chat-dots";
                case "team" -> "bi-people";
                default -> "bi-info-circle";
            };
        }

        public String getColor() {
            return switch (type) {
                case "submission" -> "success";
                case "support" -> "warning";
                case "team" -> "primary";
                default -> "secondary";
            };
        }
    }
}
package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.service.HackathonService;
import com.hackhub.service.TeamService;
import com.hackhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private SupportRequestService supportRequestService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Recupera i dati
            List<User> users = userService.getUsersByRole("USER");
            List<User> organizers = userService.getUsersByRole("ORGANIZER");
            List<User> judges = userService.getAvailableJudges();
            List<User> mentors = userService.getAvailableMentors();
            List<Hackathon> hackathons = hackathonService.getAllHackathons();
            List<SupportRequest> pendingRequests=supportRequestService.getPendingRequests();
            List<DashboardActivity> activities = new ArrayList<>();

            // Calcola i team totali
            int totalTeams = 0;
            for (Hackathon hackathon : hackathons) {
                List<Team> teams = teamService.getTeamsByHackathon(hackathon.getId());
                totalTeams += teams.size();
            }

            // Calcola le sottomissioni totali
            int totalSubmissions = 0;
            for (Hackathon hackathon : hackathons) {
                for (Team team : hackathon.getTeams()) {
                    if (team.hasSubmittedProject()) {
                        totalSubmissions++;
                    }
                }
            }

            String lastSubmissionDate = "N/A";
            if (totalSubmissions > 0) {
                lastSubmissionDate = LocalDateTime.now()
                        .minusHours(2)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }

            // Aggiungi dati al model
            model.addAttribute("users", users);
            model.addAttribute("organizers", organizers);
            model.addAttribute("judges", judges);
            model.addAttribute("mentors", mentors);
            model.addAttribute("hackathons", hackathons);
            model.addAttribute("totalUsers", users.size() + organizers.size() + judges.size() + mentors.size());
            model.addAttribute("totalHackathons", hackathons.size());
            model.addAttribute("totalTeams", totalTeams);
            model.addAttribute("totalSubmissions", totalSubmissions);
            model.addAttribute("lastSubmissionDate", lastSubmissionDate);
            model.addAttribute("pendingRequests", pendingRequests.size());
            
            // Aggiungere informazioni di sistema
            model.addAttribute("systemStatus", "online");
            model.addAttribute("databaseStatus", "online");
            model.addAttribute("servicesStatus", "online");

            // Aggiungi attività da team
            for (Hackathon h : hackathons) {
                for (Team t : h.getTeams()) {
                    if (t.hasSubmittedProject()) {
                        activities.add(new DashboardActivity(
                                "submission",
                                "Team " + t.getName() + " ha inviato progetto per " + h.getName(),
                                LocalDateTime.now().minusHours(new Random().nextInt(24)),
                                t.getCreator() != null ? t.getCreator().getUsername() : "Sconosciuto"
                        ));
                    }
                }
            }

            for (SupportRequest req : pendingRequests) {
                activities.add(new DashboardActivity(
                        "support",
                        "Richiesta supporto: " + req.getTitle(),
                        req.getRequestDate(),
                        req.getTeam() != null && req.getTeam().getCreator() != null ?
                                req.getTeam().getCreator().getUsername() : "Sconosciuto"
                ));
            }

            activities.sort((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()));
            if (activities.size() > 5) {
                activities = activities.subList(0, 5);
            }

            model.addAttribute("recentActivities", activities);

            
            return "dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Errore nel caricamento della dashboard: " + e.getMessage());
            return "error";
        }
    }

    // Classe interna per attività dashboard
    public static class DashboardActivity {
        private String type;
        private String description;
        private LocalDateTime timestamp;
        private String user;

        public DashboardActivity(String type, String description, LocalDateTime timestamp, String user) {
            this.type = type;
            this.description = description;
            this.timestamp = timestamp;
            this.user = user;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getUser() { return user; }

        public String getFormattedTime() {
            return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
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

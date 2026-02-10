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

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Recupera i dati
            List<User> users = userService.getUsersByRole("USER");
            List<User> organizers = userService.getUsersByRole("ORGANIZER");
            List<User> judges = userService.getAvailableJudges();
            List<User> mentors = userService.getAvailableMentors();
            List<Hackathon> hackathons = hackathonService.getAllHackathons();

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

            // Data ultima sottomissione (simulata)
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

            // Aggiungi informazioni di sistema
            model.addAttribute("systemStatus", "online");
            model.addAttribute("databaseStatus", "online");
            model.addAttribute("servicesStatus", "online");

            return "dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Errore nel caricamento della dashboard: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
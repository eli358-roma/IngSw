package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.service.DashboardService;
import com.hackhub.service.HackathonService;
import com.hackhub.service.SupportRequestService;
import com.hackhub.service.TeamService;
import com.hackhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Controller per la dashboard principale.
 * Gestisce la visualizzazione delle statistiche aggregate per organizzatori.
 * Utilizza DashboardService per separare la logica di business.
 */

@Controller
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private SupportRequestService supportRequestService;

    /**
     * Dashboard principale
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Ottieni tutti i dati dal service
            DashboardService.DashboardData data = dashboardService.getDashboardData();

            //statistiche principali
            model.addAttribute("users", data.getUsers());
            model.addAttribute("organizers", data.getOrganizers());
            model.addAttribute("judges", data.getJudges());
            model.addAttribute("mentors", data.getMentors());
            model.addAttribute("hackathons", data.getHackathons());
            model.addAttribute("pendingRequests", data.getPendingRequests());

            model.addAttribute("totalUsers", data.getTotalUsers());
            model.addAttribute("totalHackathons", data.getTotalHackathons());
            model.addAttribute("totalTeams", data.getTotalTeams());
            model.addAttribute("totalSubmissions", data.getTotalSubmissions());
            model.addAttribute("teamsWithProject", data.getTeamsWithProject());
            model.addAttribute("totalPendingRequests", data.getTotalPendingRequests());

            model.addAttribute("submissionRate", data.getSubmissionRate());

            model.addAttribute("lastSubmissionDate", data.getLastSubmissionDate());
            model.addAttribute("lastUpdated", data.getFormattedLastUpdated());

            model.addAttribute("systemStatus", data.getSystemStatus());
            model.addAttribute("databaseStatus", data.getDatabaseStatus());
            model.addAttribute("servicesStatus", data.getServicesStatus());

            model.addAttribute("hackathonStatusStats", data.getHackathonStatusStats());

            model.addAttribute("evaluationStats", data.getEvaluationStats());

            model.addAttribute("recentActivities", data.getRecentActivities());

            model.addAttribute("topTeams", dashboardService.getTopTeams(5));

            return "dashboard";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Errore nel caricamento della dashboard: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Dashboard JSON (per API REST)
     */
    @GetMapping("/api/dashboard")
    @ResponseBody
    public DashboardService.DashboardData dashboardApi() {
        return dashboardService.getDashboardData();
    }

    /**
     * Dashboard semplificata
     */
    @GetMapping("/dashboard/simple")
    public String dashboardSimple(Model model) {
        try {
            DashboardService.DashboardData data = dashboardService.getDashboardData();

            model.addAttribute("totalUsers", data.getTotalUsers());
            model.addAttribute("totalHackathons", data.getTotalHackathons());
            model.addAttribute("totalTeams", data.getTotalTeams());
            model.addAttribute("totalSubmissions", data.getTotalSubmissions());
            model.addAttribute("recentActivities", data.getRecentActivities());

            return "dashboard-simple";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/dashboard/stats")
    @ResponseBody
    public DashboardStats getQuickStats() {
        DashboardStats stats = new DashboardStats();

        List<Hackathon> hackathons = hackathonService.getAllHackathons();
        int totalTeams = 0;
        int totalSubmissions = 0;

        for (Hackathon h : hackathons) {
            List<Team> teams = teamService.getTeamsByHackathon(h.getId());
            totalTeams += teams.size();
            totalSubmissions += teams.stream().filter(Team::hasSubmittedProject).count();
        }

        stats.setTotalHackathons(hackathons.size());
        stats.setTotalTeams(totalTeams);
        stats.setTotalSubmissions(totalSubmissions);
        stats.setPendingRequests(supportRequestService.getPendingRequests().size());

        return stats;
    }

    public static class DashboardStats {
        private int totalHackathons;
        private int totalTeams;
        private int totalSubmissions;
        private int pendingRequests;

        public void setTotalHackathons(int totalHackathons) { this.totalHackathons = totalHackathons; }

        public void setTotalTeams(int totalTeams) { this.totalTeams = totalTeams; }

        public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }

        public void setPendingRequests(int pendingRequests) { this.pendingRequests = pendingRequests; }
    }
}

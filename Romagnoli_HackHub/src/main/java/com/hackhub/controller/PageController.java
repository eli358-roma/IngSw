package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.service.HackathonService;
import com.hackhub.service.TeamService;
import com.hackhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class PageController {

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    // ========== HACKATHON PAGES ==========

    @GetMapping("/hackathons")
    public String hackathonsPage(Model model) {
        List<Hackathon> hackathons = hackathonService.getAllHackathons();
        model.addAttribute("hackathons", hackathons);
        return "hackathons/list";
    }

    @GetMapping("/hackathons/create")
    public String createHackathonPage(Model model) {
        // Prepara i dati necessari per il form di creazione
        model.addAttribute("organizers", userService.getUsersByRole("ORGANIZER"));
        model.addAttribute("judges", userService.getAvailableJudges());
        model.addAttribute("mentors", userService.getAvailableMentors());
        return "hackathons/create";
    }

    @GetMapping("/hackathons/{id}")
    public String hackathonDetailPage(@PathVariable Long id, Model model) {
        Hackathon hackathon = hackathonService.getHackathonById(id); // Dovrai aggiungere questo metodo nel service
        model.addAttribute("hackathon", hackathon);
        model.addAttribute("teams", teamService.getTeamsByHackathon(id));
        return "hackathons/detail";
    }

    @GetMapping("/hackathons/{id}/manage")
    public String manageHackathonPage(@PathVariable Long id, Model model) {
        Hackathon hackathon = hackathonService.getHackathonById(id);
        model.addAttribute("hackathon", hackathon);
        model.addAttribute("availableJudges", userService.getAvailableJudges());
        model.addAttribute("availableMentors", userService.getAvailableMentors());
        return "hackathons/manage";
    }

    // ========== TEAM PAGES ==========

    @GetMapping("/teams")
    public String teamsPage(Model model) {
        model.addAttribute("teams", teamService.getAllTeams());
        return "teams/list";
    }

    @GetMapping("/teams/create")
    public String createTeamPage(Model model) {
        model.addAttribute("hackathons", hackathonService.getAllHackathons());
        model.addAttribute("users", userService.getUsersByRole("USER"));
        return "teams/create";
    }

    @GetMapping("/teams/{id}")
    public String teamDetailPage(@PathVariable Long id, Model model) {
        Team team = teamService.getTeamById(id);
        model.addAttribute("team", team);
        return "teams/detail";
    }

    // ========== SUBMISSIONS PAGES ==========

    @GetMapping("/submissions")
    public String submissionsPage(Model model) {
        model.addAttribute("hackathons", hackathonService.getAllHackathons());
        return "submissions/list";
    }

    @GetMapping("/submissions/review")
    public String reviewSubmissionsPage(Model model) {
        // Filtra solo hackathon in valutazione
        model.addAttribute("hackathons", hackathonService.getHackathonsByStatus("IN_VALUTAZIONE"));
        return "submissions/review";
    }

    // ========== JUDGES PAGES ==========

    @GetMapping("/judges")
    public String judgesPage(Model model) {
        model.addAttribute("judges", userService.getAvailableJudges());
        return "judges/list";
    }

    // ========== MENTORS PAGES ==========

    @GetMapping("/mentors")
    public String mentorsPage(Model model) {
        model.addAttribute("mentors", userService.getAvailableMentors());
        return "mentors/list";
    }

    @GetMapping("/support/requests")
    public String supportRequestsPage(Model model) {
        // Per ora vuoto, implementeremo dopo
        return "support/requests";
    }

    // ========== REPORTS PAGES ==========

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        model.addAttribute("hackathons", hackathonService.getAllHackathons());
        return "reports/list";
    }

    @GetMapping("/reports/generate")
    public String generateReportPage(Model model) {
        return "reports/generate";
    }

    // ========== ADMIN PAGES ==========

    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("hackathons", hackathonService.getAllHackathons());
        return "admin/dashboard";
    }

    // ========== USER PROFILE ==========

    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "profile/index";
    }
}
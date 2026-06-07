package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.service.HackathonService;
import com.hackhub.service.TeamService;
import com.hackhub.service.UserService;
import jakarta.servlet.http.HttpSession;
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

    //hackathon pages
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
        Hackathon hackathon = hackathonService.getHackathonById(id);
        model.addAttribute("hackathon", hackathon);
        model.addAttribute("teams", teamService.getTeamsByHackathon(id));
        return "hackathons/detail";
    }

    @GetMapping("/hackathons/{id}/manage")
    public String manageHackathonPage(@PathVariable Long id, Model model, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return "redirect:/login";
            }

            // Recupera l'hackathon
            Hackathon hackathon = hackathonService.getHackathonById(id);

            // Verifica che l'utente sia l'organizzatore
            if (!hackathon.getOrganizer().getId().equals(userId)) {
                model.addAttribute("error", "Solo l'organizzatore può gestire questo hackathon");
                return "error";
            }

            // Aggiungi i dati necessari per i select
            model.addAttribute("hackathon", hackathon);
            model.addAttribute("availableJudges", userService.getAvailableJudges());
            model.addAttribute("availableMentors", userService.getAvailableMentors());

            return "hackathons/manage";

        } catch (Exception e) {
            model.addAttribute("error", "Errore nel caricamento della pagina: " + e.getMessage());
            return "error";
        }
    }

    //Pagina Team
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

    @GetMapping("/teams/manage/{id}")
    public String manageTeamPage(@PathVariable Long id, Model model, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return "redirect:/login";
            }

            Team team = teamService.getTeamById(id);

            // Verifica che l'utente sia il creatore
            if (!team.getCreator().getId().equals(userId)) {
                model.addAttribute("error", "Solo il creatore del team può gestire i membri");
                return "error";
            }

            List<User> availableUsers = teamService.getAvailableUsersForTeam(id);

            model.addAttribute("team", team);
            model.addAttribute("availableUsers", availableUsers);

            return "teams/manage";

        } catch (Exception e) {
            model.addAttribute("error", "Errore: " + e.getMessage());
            return "error";
        }
    }

    //Pagina giudici
    @GetMapping("/judges")
    public String judgesPage(Model model) {
        model.addAttribute("judges", userService.getAvailableJudges());
        return "judges/list";
    }

    //pagina mentori
    @GetMapping("/mentors")
    public String mentorsPage(Model model) {
        model.addAttribute("mentors", userService.getAvailableMentors());
        return "mentors/list";
    }

    @GetMapping("/support/requests")
    public String supportRequestsPage(Model model) {
        return "support/requests";
    }

    //Pagina report

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        model.addAttribute("hackathons", hackathonService.getAllHackathons());
        return "reports/list";
    }

    @GetMapping("/reports/generate")
    public String generateReportPage(Model model) {
        return "reports/generate";
    }

    //Pagina Admin
    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("hackathons", hackathonService.getAllHackathons());
        return "admin/dashboard";
    }

    //profilo user
    @GetMapping("/profile")
    public String profilePage(Model model) {
        return "profile/index";
    }

    @GetMapping("/my-invites")
    public String myInvitesPage() {
        return "user/invites";
    }
}

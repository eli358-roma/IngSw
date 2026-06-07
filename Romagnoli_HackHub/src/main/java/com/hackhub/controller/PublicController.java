package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.User;
import com.hackhub.service.HackathonService;
import com.hackhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller per le pagine pubbliche
 */
@Controller
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private UserService userService;

    //Pagina home
    @GetMapping
    public String home(Model model) {
        List<Hackathon> allHackathons = hackathonService.getAllHackathons();

        List<Hackathon> activeHackathons = allHackathons.stream()
                .filter(h -> "INSCRIZIONE".equals(h.getStatus()) || "IN_CORSO".equals(h.getStatus()))
                .limit(3)
                .toList();

        model.addAttribute("hackathons", allHackathons);
        model.addAttribute("activeHackathons", activeHackathons);
        model.addAttribute("totalHackathons", allHackathons.size());

        return "public/home";
    }


    @GetMapping("/hackathon/{id}")
    public String hackathonDetail(@PathVariable Long id, Model model) {
        Hackathon hackathon = hackathonService.getHackathonById(id);
        model.addAttribute("hackathon", hackathon);
        model.addAttribute("totalTeams", hackathon.getTeams() != null ? hackathon.getTeams().size() : 0);
        return "public/hackathon-detail";
    }

    @GetMapping("/hackathons")
    public String listHackathons(Model model) {
        List<Hackathon> hackathons = hackathonService.getAllHackathons();
        model.addAttribute("hackathons", hackathons);
        return "public/hackathons";
    }

    @GetMapping("/judges")
    public String listJudges(Model model) {
        List<User> judges = userService.getAvailableJudges();
        model.addAttribute("judges", judges);
        model.addAttribute("totalJudges", judges.size());
        return "public/judges";
    }

    @GetMapping("/judge/{id}")
    public String judgeDetail(@PathVariable Long id, Model model) {
        User judge = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Giudice non trovato"));

        if (!"JUDGE".equals(judge.getRole())) {
            throw new RuntimeException("L'utente non è un giudice");
        }

        model.addAttribute("judge", judge);
        return "public/judge-detail";
    }


    @GetMapping("/mentors")
    public String listMentors(Model model) {
        List<User> mentors = userService.getAvailableMentors();
        model.addAttribute("mentors", mentors);
        model.addAttribute("totalMentors", mentors.size());
        return "public/mentors";
    }

    @GetMapping("/mentor/{id}")
    public String mentorDetail(@PathVariable Long id, Model model) {
        User mentor = userService.getUserById(id)
                .orElseThrow(() -> new RuntimeException("Mentor non trovato"));

        if (!"MENTOR".equals(mentor.getRole())) {
            throw new RuntimeException("L'utente non è un mentor");
        }

        model.addAttribute("mentor", mentor);
        return "public/mentor-detail";
    }


    @GetMapping("/teams")
    public String listTeams(Model model) {
        // Solo team che hanno inviato progetti
        List<com.hackhub.model.Team> allTeams = new java.util.ArrayList<>();
        for (Hackathon h : hackathonService.getAllHackathons()) {
            allTeams.addAll(h.getTeams());
        }

        List<com.hackhub.model.Team> teamsWithProjects = allTeams.stream()
                .filter(com.hackhub.model.Team::hasSubmittedProject)
                .toList();

        model.addAttribute("teams", teamsWithProjects);
        model.addAttribute("totalTeams", teamsWithProjects.size());
        return "public/teams";
    }


    @GetMapping("/about")
    public String about() {
        return "public/about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "public/contact";
    }

    @GetMapping("/faq")
    public String faq() {
        return "public/faq";
    }
}
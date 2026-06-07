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
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/submission")
public class SubmissionController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private UserService userService;

    /**
     * Visualizza i dettagli di una sottomissione
     * Accessibile in base al ruolo dell'utente
     */
    @GetMapping("/{teamId}")
    public String viewSubmission(@PathVariable Long teamId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return "redirect:/login";

        Team team = teamService.getTeamById(teamId);
        Hackathon hackathon = team.getHackathon();

        boolean canView = false;
        if ("ORGANIZER".equals(userRole) && hackathon.getOrganizer().getId().equals(userId)) {
            canView = true;
        } else if ("JUDGE".equals(userRole) && hackathon.getJudge() != null &&
                hackathon.getJudge().getId().equals(userId)) {
            canView = true;
        } else if ("MENTOR".equals(userRole) && hackathon.getMentors().stream()
                .anyMatch(m -> m.getId().equals(userId))) {
            canView = true;
        } else if (team.getMembers().stream().anyMatch(m -> m.getId().equals(userId))) {
            canView = true;
        }

        if (!canView) {
            model.addAttribute("error", "Non hai i permessi per visualizzare questa sottomissione");
            return "error";
        }

        model.addAttribute("team", team);
        model.addAttribute("hackathon", hackathon);
        model.addAttribute("canEvaluate",
                "JUDGE".equals(userRole) &&
                        hackathon.getJudge() != null &&
                        hackathon.getJudge().getId().equals(userId) &&
                        !team.isEvaluated() &&
                        "IN_VALUTAZIONE".equals(hackathon.getStatus())
        );

        return "submission/detail";
    }

    /**
     * Lista di tutte le sottomissioni
     */
    @GetMapping
    public String listSubmissions(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) {
            return "redirect:/login";
        }

        User currentUser = userService.getUserById(userId).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<SubmissionInfo> submissions = new ArrayList<>();

        switch (userRole) {
            case "ORGANIZER":
                // Organizzatore vede tutte le sottomissioni dei suoi hackathon
                List<Hackathon> organizerHackathons =
                        hackathonService.getHackathonsByOrganizer(userId);
                for (Hackathon h : organizerHackathons) {
                    if (h.getTeams() != null) {
                        for (Team t : h.getTeams()) {
                            if (t.hasSubmittedProject()) {
                                submissions.add(new SubmissionInfo(t, h));
                            }
                        }
                    }
                }
                break;

            case "JUDGE":
                // Giudice vede le sottomissioni degli hackathon a lui assegnati
                List<Hackathon> allHackathons = hackathonService.getAllHackathons();
                for (Hackathon h : allHackathons) {
                    if (h.getJudge() != null && h.getJudge().getId().equals(userId)) {
                        if (h.getTeams() != null) {
                            for (Team t : h.getTeams()) {
                                if (t.hasSubmittedProject()) {
                                    submissions.add(new SubmissionInfo(t, h));
                                }
                            }
                        }
                    }
                }
                break;

            case "MENTOR":
                // Mentore vede le sottomissioni degli hackathon a lui assegnati
                List<Hackathon> mentorHackathons = hackathonService.getAllHackathons()
                        .stream()
                        .filter(h -> h.getMentors() != null &&
                                h.getMentors().stream().anyMatch(m -> m.getId().equals(userId)))
                        .collect(Collectors.toList());
                for (Hackathon h : mentorHackathons) {
                    if (h.getTeams() != null) {
                        for (Team t : h.getTeams()) {
                            if (t.hasSubmittedProject()) {
                                submissions.add(new SubmissionInfo(t, h));
                            }
                        }
                    }
                }
                break;

            case "USER":
                // Membro del team vede solo le sottomissioni dei propri team
                List<Team> userTeams = teamService.findTeamsByMember(userId);
                for (Team t : userTeams) {
                    if (t.hasSubmittedProject()) {
                        submissions.add(new SubmissionInfo(t, t.getHackathon()));
                    }
                }
                break;

            default:
                // Altri ruoli non vedono nulla
                break;
        }

        model.addAttribute("submissions", submissions);
        model.addAttribute("userRole", userRole);
        model.addAttribute("totalSubmissions", submissions.size());

        return "submission/list";
    }

    /**
     * Classe interna per le informazioni della sottomissione
     */
    public static class SubmissionInfo {
        private Team team;
        private Hackathon hackathon;

        public SubmissionInfo(Team team, Hackathon hackathon) {
            this.team = team;
            this.hackathon = hackathon;
        }

        public Team getTeam() { return team; }
        public Hackathon getHackathon() { return hackathon; }

        public Double getScore() {
            return team != null ? team.getScore() : null;
        }

        public Long getTeamId() {
            return team != null ? team.getId() : null;
        }
    }
}
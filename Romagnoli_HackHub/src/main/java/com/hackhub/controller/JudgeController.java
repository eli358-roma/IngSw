package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.service.HackathonService;
import com.hackhub.service.TeamService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/judge")
public class JudgeController {

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private TeamService teamService;

    @GetMapping("/dashboard")
    public String judgeDashboard(HttpSession session, Model model) {
        Long judgeId = (Long) session.getAttribute("userId");

        if (judgeId == null) {
            return "redirect:/login";
        }

        // Trova gli hackathon dove questo utente è giudice
        List<Hackathon> hackathons = hackathonService.getAllHackathons().stream()
                .filter(h -> h.getJudge() != null && h.getJudge().getId().equals(judgeId))
                .collect(Collectors.toList());

        model.addAttribute("hackathons", hackathons);
        return "judge/dashboard";
    }

    @GetMapping("/hackathon/{hackathonId}")
    public String viewHackathon(@PathVariable Long hackathonId,
                                HttpSession session,
                                Model model) {
        Long judgeId = (Long) session.getAttribute("userId");
        Hackathon hackathon = hackathonService.getHackathonById(hackathonId);

        // Verifica che sia il giudice corretto
        if (hackathon.getJudge() == null || !hackathon.getJudge().getId().equals(judgeId)) {
            return "redirect:/judge/dashboard?error=not_authorized";
        }

        List<Team> allTeams = teamService.getTeamsByHackathon(hackathonId);

        //filtro solo team che hanno inviato progetto
        List<Team> teamsWithSubmission = allTeams.stream()
                .filter(Team::hasSubmittedProject)
                .collect(Collectors.toList());

        // Separa i team valutati e quelli non valutati
        List<Team> pendingTeams = teamsWithSubmission.stream()
                .filter(t -> !t.isEvaluated())
                .collect(Collectors.toList());

        List<Team> evaluatedTeams = teamsWithSubmission.stream()
                .filter(Team::isEvaluated)
                .collect(Collectors.toList());

        model.addAttribute("hackathon", hackathon);
        model.addAttribute("pendingTeams", pendingTeams);
        model.addAttribute("evaluatedTeams", evaluatedTeams);
        model.addAttribute("totalTeams", allTeams.size());
        model.addAttribute("submittedTeams", teamsWithSubmission.size());

        return "judge/hackathon";
    }

    @PostMapping("/evaluate")
    public String evaluate(@RequestParam Long teamId,
                           @RequestParam Double score,
                           @RequestParam String feedback,
                           HttpSession session) {
        try {
            Long judgeId = (Long) session.getAttribute("userId");

            // Validazione punteggio
            if (score < 0 || score > 10) {
                throw new RuntimeException("Il punteggio deve essere tra 0 e 10");
            }

            Team team = teamService.getTeamById(teamId);
            Long hackathonId = team.getHackathon().getId();

            teamService.evaluateTeam(teamId, score, feedback, judgeId);

            return "redirect:/judge/hackathon/" + hackathonId + "?success=valutazione_effettuata";

        } catch (Exception e) {
            Team team = teamService.getTeamById(teamId);
            return "redirect:/judge/hackathon/" + team.getHackathon().getId() + "?error=" + e.getMessage();
        }
    }

    @GetMapping("/judge")
    public String redirectToJudgeDashboard() {
        return "redirect:/judge/dashboard";
    }
}
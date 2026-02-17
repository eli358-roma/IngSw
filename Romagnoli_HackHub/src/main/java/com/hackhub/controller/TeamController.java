package com.hackhub.controller;

import com.hackhub.model.Team;
import com.hackhub.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Map<String, Object> request) {
        String teamName = (String) request.get("teamName");
        Long hackathonId = Long.valueOf(request.get("hackathonId").toString());
        Long creatorId = Long.valueOf(request.get("creatorId").toString());

        // Usa il metodo diretto del service
        Team team = teamService.createTeam(teamName, hackathonId, creatorId);
        return ResponseEntity.ok(team);
    }

    @PostMapping("/{teamId}/join")
    public ResponseEntity<String> joinTeam(@PathVariable Long teamId, @RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        teamService.joinTeam(teamId, userId);
        return ResponseEntity.ok("Aggiunto al team con successo");
    }

    @PostMapping("/{teamId}/leave")
    public ResponseEntity<String> leaveTeam(@PathVariable Long teamId, @RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        teamService.leaveTeam(teamId, userId);
        return ResponseEntity.ok("Rimosso dal team con successo");
    }

    @PutMapping("/{teamId}/submit")
    public ResponseEntity<String> submitProject(@PathVariable Long teamId, @RequestBody Map<String, String> request) {
        String projectName = request.get("projectName");
        String description = request.get("description");
        String repoUrl = request.get("repositoryUrl");

        teamService.submitProject(teamId, projectName, description, repoUrl);
        return ResponseEntity.ok("Progetto inviato con successo");
    }

    @PutMapping("/{teamId}/evaluate")
    public ResponseEntity<String> evaluateTeam(@PathVariable Long teamId, @RequestBody Map<String, Object> request) {
        Double score = Double.valueOf(request.get("score").toString());
        String feedback = (String) request.get("feedback");

        teamService.evaluateTeam(teamId, score, feedback);
        return ResponseEntity.ok("Team valutato con successo");
    }

    @GetMapping("/hackathon/{hackathonId}")
    public ResponseEntity<List<Team>> getTeamsByHackathon(@PathVariable Long hackathonId) {
        return ResponseEntity.ok(teamService.getTeamsByHackathon(hackathonId));
    }

    @GetMapping("/hackathon/{hackathonId}/statistics")
    public ResponseEntity<TeamService.TeamStatistics> getTeamStatistics(@PathVariable Long hackathonId) {
        return ResponseEntity.ok(teamService.getTeamStatistics(hackathonId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Team>> getTeamsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(teamService.findTeamsByMember(userId));
    }

    @GetMapping("/{teamId}/can-join/{userId}")
    public ResponseEntity<Map<String, Object>> canJoinTeam(
            @PathVariable Long teamId,
            @PathVariable Long userId) {

        boolean canJoin = teamService.canJoinTeam(teamId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("canJoin", canJoin);

        if (!canJoin) {
            Team team = teamService.getTeamById(teamId);

            if (team.isFull()) {
                response.put("reason", "Team al completo");
            } else {
                response.put("reason", "Utente già in un altro team");
            }
        }

        return ResponseEntity.ok(response);
    }

}

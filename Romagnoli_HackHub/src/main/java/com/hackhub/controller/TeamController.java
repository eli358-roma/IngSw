package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.repository.UserRepository;
import com.hackhub.service.TeamService;
import com.hackhub.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Map<String, Object> request) {
        String teamName = (String) request.get("teamName");
        Long hackathonId = Long.valueOf(request.get("hackathonId").toString());
        Long creatorId = Long.valueOf(request.get("creatorId").toString());

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
    public ResponseEntity<String> submitProject(@PathVariable Long teamId,
                                                @RequestBody Map<String, String> request) {
        try {
            String projectName = request.get("projectName");
            String description = request.get("description");
            String repoUrl = request.get("repositoryUrl");

            // Validazione
            if (projectName == null || projectName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Il nome del progetto è obbligatorio");
            }
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("La descrizione è obbligatoria");
            }
            if (repoUrl == null || repoUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("L'URL del repository è obbligatorio");
            }

            teamService.submitProject(teamId, projectName, description, repoUrl);
            return ResponseEntity.ok("Progetto inviato con successo");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore: " + e.getMessage());
        }
    }

    @PutMapping("/{teamId}/evaluate")
    public ResponseEntity<String> evaluateTeam(@PathVariable Long teamId,
                                               @RequestBody Map<String, Object> request) {
        Double score = Double.valueOf(request.get("score").toString());
        String feedback = (String) request.get("feedback");
        Long judgeId = Long.valueOf(request.get("judgeId").toString());

        teamService.evaluateTeam(teamId, score, feedback, judgeId);
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

    @PostMapping("/{teamId}/add-member")
    public ResponseEntity<String> addMember(@PathVariable Long teamId,
                                            @RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            Long currentUserId = request.get("currentUserId");

            Team team = teamService.getTeamById(teamId);

            if (!team.getCreator().getId().equals(currentUserId)) {
                return ResponseEntity.status(403).body("Solo il creatore del team può aggiungere membri");
            }

            teamService.joinTeam(teamId, userId);
            return ResponseEntity.ok("Membro aggiunto con successo");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{teamId}/available-users")
    public ResponseEntity<List<User>> getAvailableUsersForTeam(@PathVariable Long teamId) {
        Team team = teamService.getTeamById(teamId);
        Hackathon hackathon = team.getHackathon();

        List<User> allUsers = userService.getUsersByRole("USER");
        List<User> availableUsers = allUsers.stream()
                .filter(u -> u.getTeam() == null)
                .filter(u -> !team.hasMember(u))
                .collect(Collectors.toList());

        return ResponseEntity.ok(availableUsers);
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

    @GetMapping("/{teamId}/manage")
    public ResponseEntity<Map<String, Object>> getTeamManageData(@PathVariable Long teamId) {
        Team team = teamService.getTeamById(teamId);
        List<User> availableUsers = teamService.getAvailableUsersForTeam(teamId);

        Map<String, Object> response = new HashMap<>();
        response.put("team", team);
        response.put("availableUsers", availableUsers);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/invite")
    public ResponseEntity<String> inviteUser(@PathVariable Long teamId,
                                             @RequestBody Map<String, Long> request) {
        try {
            Long invitedUserId = request.get("userId");
            Team team = teamService.getTeamById(teamId);
            User invitedUser = userService.getUserById(invitedUserId).orElse(null);

            if (invitedUser == null) {
                return ResponseEntity.badRequest().body("Utente non trovato");
            }

            // Verifica che l'utente non sia già in un team
            if (invitedUser.getTeam() != null) {
                return ResponseEntity.badRequest().body("L'utente è già in un team");
            }

            invitedUser.addPendingInvite(teamId);
            userRepository.save(invitedUser);

            return ResponseEntity.ok("Invito inviato a " + invitedUser.getUsername());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore: " + e.getMessage());
        }
    }

    @PostMapping("/invites/{teamId}/accept")
    public ResponseEntity<String> acceptInvite(@PathVariable Long teamId, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            User user = userService.getUserById(userId).orElse(null);

            if (user == null) {
                return ResponseEntity.status(401).body("Utente non trovato");
            }

            if (!user.getPendingInvites().contains(teamId)) {
                return ResponseEntity.badRequest().body("Nessun invito in sospeso per questo team");
            }

            Team team = teamService.getTeamById(teamId);

            if (team.isFull()) {
                return ResponseEntity.badRequest().body("Il team è al completo");
            }

            user.removePendingInvite(teamId);
            teamService.joinTeam(teamId, userId);

            return ResponseEntity.ok("Invito accettato! Ora fai parte del team " + team.getName());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore: " + e.getMessage());
        }
    }

    @GetMapping("/my-invites")
    public ResponseEntity<List<Team>> getMyInvites(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getUserById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<Team> invitedTeams = new ArrayList<>();
        for (Long teamId : user.getPendingInvites()) {
            try {
                invitedTeams.add(teamService.getTeamById(teamId));
            } catch (Exception e) {

            }
        }

        return ResponseEntity.ok(invitedTeams);
    }
}

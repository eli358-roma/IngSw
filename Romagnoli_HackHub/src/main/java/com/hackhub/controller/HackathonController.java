package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.service.HackathonService;
import com.hackhub.service.NotificationService;
import com.hackhub.service.TeamService;
import com.hackhub.repository.HackathonRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hackathons")
@CrossOrigin(origins = "*")
public class HackathonController {

    @Autowired
    private HackathonService hackathonService;

    @Autowired
    private HackathonRepository hackathonRepository;

    @Autowired
    private TeamService teamService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Hackathon> createHackathon(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String rules = (String) request.get("rules");

        LocalDateTime regDeadline = LocalDateTime.parse((String) request.get("registrationDeadline"));
        LocalDateTime startDate = LocalDateTime.parse((String) request.get("startDate"));
        LocalDateTime endDate = LocalDateTime.parse((String) request.get("endDate"));

        Integer maxTeamSize = Integer.valueOf(request.get("maxTeamSize").toString());
        Long organizerId = Long.valueOf(request.get("organizerId").toString());

        //Gestione premio
        Double prizeMoney = 0.0;
        if (request.containsKey("prizeMoney") && request.get("prizeMoney") != null) {
            prizeMoney = Double.valueOf(request.get("prizeMoney").toString());
        }

        Hackathon hackathon = hackathonService.createHackathon(
                name, description, rules, regDeadline, startDate, endDate,
                maxTeamSize, organizerId, prizeMoney);

        return ResponseEntity.ok(hackathon);
    }

    @PutMapping("/{id}/assign-judge")
    public ResponseEntity<Hackathon> assignJudge(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long judgeId = request.get("judgeId");
        return ResponseEntity.ok(hackathonService.assignJudge(id, judgeId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Hackathon> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String newStatus = request.get("status");
        return ResponseEntity.ok(hackathonService.updateStatus(id, newStatus));
    }

    @PostMapping("/{id}/declare-winner")
    public ResponseEntity<String> declareWinner(@PathVariable Long id,
                                                @RequestBody Map<String, Long> request,
                                                HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            Long teamId = request.get("teamId");

            Hackathon hackathon = hackathonService.getHackathonById(id);

            // Viene verificato che l'utente sia l'organizzatore
            if (!hackathon.getOrganizer().getId().equals(userId)) {
                return ResponseEntity.status(403).body("Solo l'organizzatore può proclamare il vincitore");
            }

            //Viene verificato che l'hackathon sia concluso
            if (!"CONCLUSO".equals(hackathon.getStatus())) {
                return ResponseEntity.badRequest().body("L'hackathon deve essere concluso");
            }

            Team team = teamService.getTeamById(teamId);
            hackathon.setWinnerTeamId(teamId);
            hackathonRepository.save(hackathon);

            //notifica il team vincitore
            String message = "Congratulazioni! Il tuo team " + team.getName() +
                    " ha vinto l'hackathon " + hackathon.getName() + "!";
            for (User member : team.getMembers()) {
                notificationService.sendNotification("EMAIL", message, member);
            }

            return ResponseEntity.ok("Vincitore proclamato: " + team.getName());

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Hackathon>> getAllHackathons() {
        return ResponseEntity.ok(hackathonService.getAllHackathons());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Hackathon>> getHackathonsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(hackathonService.getHackathonsByStatus(status));
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<Hackathon>> getHackathonsByOrganizer(@PathVariable Long organizerId) {
        return ResponseEntity.ok(hackathonService.getHackathonsByOrganizer(organizerId));
    }

    @PostMapping("/{id}/add-mentor")
    public ResponseEntity<Hackathon> addMentor(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long mentorId = request.get("mentorId");
        return ResponseEntity.ok(hackathonService.addMentor(id, mentorId));
    }

    @DeleteMapping("/{id}/remove-mentor/{mentorId}")
    public ResponseEntity<Hackathon> removeMentor(@PathVariable Long id, @PathVariable Long mentorId) {
        return ResponseEntity.ok(hackathonService.removeMentor(id, mentorId));
    }

    @GetMapping("/{id}/mentors")
    public ResponseEntity<List<User>> getMentors(@PathVariable Long id) {
        return ResponseEntity.ok(hackathonService.getMentors(id));
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getHackathonStatistics(@PathVariable Long id) {
        return ResponseEntity.ok(hackathonService.getHackathonStatistics(id));
    }

    @GetMapping("/{id}/can-access/{userId}")
    public ResponseEntity<Map<String, Boolean>> checkAccess(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam String userRole) {

        boolean canAccess = hackathonService.canAccessHackathon(id, userId, userRole);

        Map<String, Boolean> response = new HashMap<>();
        response.put("canAccess", canAccess);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status-summary")
    public ResponseEntity<Map<String, Long>> getStatusSummary() {
        Map<String, Long> summary = new HashMap<>();

        for (String status : List.of("INSCRIZIONE", "IN_CORSO", "IN_VALUTAZIONE", "CONCLUSO")) {
            summary.put(status, (long) hackathonService.getHackathonsByStatus(status).size());
        }

        return ResponseEntity.ok(summary);
    }
}

package com.hackhub.controller;

import com.hackhub.model.Hackathon;
import com.hackhub.model.User;
import com.hackhub.service.HackathonService;
import com.hackhub.repository.HackathonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
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

        Hackathon hackathon = hackathonService.createHackathon(
                name, description, rules, regDeadline, startDate, endDate, maxTeamSize, organizerId);

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

    @PutMapping("/{id}/declare-winner")
    public ResponseEntity<Hackathon> declareWinner(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long teamId = request.get("teamId");
        return ResponseEntity.ok(hackathonService.declareWinner(id, teamId));
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
}
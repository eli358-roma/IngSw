package com.hackhub.controller;

import com.hackhub.model.SupportRequest;
import com.hackhub.service.SupportRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/support-requests")
@CrossOrigin(origins = "*")
public class SupportRequestController {

    @Autowired
    private SupportRequestService supportRequestService;

    @PostMapping
    public ResponseEntity<SupportRequest> createRequest(@RequestBody Map<String, Object> request) {
        Long teamId = Long.valueOf(request.get("teamId").toString());
        String title = (String) request.get("title");
        String description = (String) request.get("description");

        return ResponseEntity.ok(supportRequestService.createSupportRequest(teamId, title, description));
    }

    @PutMapping("/{id}/assign-mentor")
    public ResponseEntity<SupportRequest> assignMentor(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long mentorId = request.get("mentorId");
        return ResponseEntity.ok(supportRequestService.assignMentor(id, mentorId));
    }

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<List<SupportRequest>> getRequestsForMentor(@PathVariable Long mentorId) {
        return ResponseEntity.ok(supportRequestService.getRequestsByMentor(mentorId));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<SupportRequest> resolveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(supportRequestService.resolveRequest(id));
    }
}
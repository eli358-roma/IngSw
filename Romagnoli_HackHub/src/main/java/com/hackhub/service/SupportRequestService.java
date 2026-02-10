package com.hackhub.service;

import com.hackhub.model.SupportRequest;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.repository.SupportRequestRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupportRequestService {

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepository;

    public SupportRequest createSupportRequest(Long teamId, String title, String description) {
        Team team = teamService.getTeamById(teamId);

        SupportRequest request = new SupportRequest();
        request.setTeam(team);
        request.setTitle(title);
        request.setDescription(description);
        request.setRequestDate(LocalDateTime.now());
        request.setStatus("PENDING");

        return supportRequestRepository.save(request);
    }

    public SupportRequest assignMentor(Long requestId, Long mentorId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Richiesta non trovata"));

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor non trovato"));

        if (!"MENTOR".equals(mentor.getRole())) {
            throw new RuntimeException("L'utente non è un mentor");
        }

        request.setMentor(mentor);
        request.setStatus("ASSIGNED");

        return supportRequestRepository.save(request);
    }

    public SupportRequest resolveRequest(Long requestId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Richiesta non trovata"));

        request.setStatus("RESOLVED");

        return supportRequestRepository.save(request);
    }

    public List<SupportRequest> getRequestsByMentor(Long mentorId) {
        return supportRequestRepository.findByMentorId(mentorId);
    }

    public List<SupportRequest> getRequestsByTeam(Long teamId) {
        return supportRequestRepository.findByTeamId(teamId);
    }

    public List<SupportRequest> getPendingRequests() {
        return supportRequestRepository.findByStatus("PENDING");
    }
}
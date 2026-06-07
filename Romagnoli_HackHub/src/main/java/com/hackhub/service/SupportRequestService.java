package com.hackhub.service;

import com.hackhub.model.Hackathon;
import com.hackhub.model.SupportRequest;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.repository.SupportRequestRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SupportRequestService {

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hackhub.pattern.facade.ExternalServiceFacade externalServiceFacade;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    public SupportRequest createSupportRequest(Long teamId, String title, String description, Long userId) {
        Team team = teamService.getTeamById(teamId);

        //verifica che l'utente sia un membro del team
        User user = userService.getUserById(userId).orElse(null);
        if (user == null || !team.hasMember(user)) {
            throw new RuntimeException("Devi essere membro del team per creare richieste");
        }

        SupportRequest request = new SupportRequest();
        request.setTeam(team);
        request.setTitle(title);
        request.setDescription(description);
        request.setRequestDate(LocalDateTime.now());
        request.setStatus("PENDING");

        return supportRequestRepository.save(request);
    }

    /**
     * Assegna un mentore a una richiesta di supporto
     */
    @Transactional
    public SupportRequest assignMentor(Long requestId, Long mentorId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Richiesta non trovata"));

        User mentor = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor non trovato"));

        if (!"MENTOR".equals(mentor.getRole())) {
            throw new RuntimeException("L'utente non è un mentor");
        }

        Hackathon hackathon = request.getTeam().getHackathon();
        if (!hackathon.getMentors().contains(mentor)) {
            throw new RuntimeException("Questo mentore non è assegnato a questo hackathon");
        }

        request.setMentor(mentor);
        request.setStatus("ASSIGNED");

        SupportRequest saved = supportRequestRepository.save(request);

        String message = "Il mentore " + mentor.getUsername() + " è stato assegnato alla tua richiesta: " + request.getTitle();
        for (User member : request.getTeam().getMembers()) {
            notificationService.sendNotification("IN_APP", message, member);
        }

        return saved;
    }

    /**
     * Ottiene tutte le richieste non assegnate per un hackathon
     */
    public List<SupportRequest> getUnassignedRequestsByHackathon(Long hackathonId) {
        return supportRequestRepository.findByTeamHackathonId(hackathonId).stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());
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

    /**
     * Ottiene tutte le richieste per un hackathon
     */
    public List<SupportRequest> getRequestsByHackathon(Long hackathonId) {
        return supportRequestRepository.findByTeamHackathonId(hackathonId);
    }

    /**
     * Programma una call per una richiesta di supporto
     */
    @Transactional
    public SupportRequest scheduleCall(Long requestId, LocalDateTime scheduledDate, Long mentorId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Richiesta non trovata"));

        if (!request.getMentor().getId().equals(mentorId)) {
            throw new RuntimeException("Non sei il mentore assegnato a questa richiesta");
        }

        //viene usato il CalendarService per programmare la call
        String eventId = externalServiceFacade.scheduleMentorCall(
                request.getMentor().getUsername(),
                request.getMentor().getEmail(),
                request.getTeam().getName(),
                request.getTeam().getCreator().getEmail(),
                scheduledDate,
                scheduledDate.plusHours(1),
                request.getTitle()
        );

        request.setScheduledDate(scheduledDate);
        request.setCalendarEventId(eventId);
        request.setStatus("SCHEDULED");

        SupportRequest saved = supportRequestRepository.save(request);

        String message = String.format(
                "Call programmata per la richiesta '%s' il %s",
                request.getTitle(),
                scheduledDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );

        for (User member : request.getTeam().getMembers()) {
            notificationService.sendNotification("EMAIL", message, member);
        }

        return saved;
    }


    /**
     * Ottiene statistiche sulle richieste
     */
    public Map<String, Object> getStatistics(Long hackathonId) {
        List<SupportRequest> requests = getRequestsByHackathon(hackathonId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", requests.size());
        stats.put("pending", requests.stream().filter(r -> "PENDING".equals(r.getStatus())).count());
        stats.put("assigned", requests.stream().filter(r -> "ASSIGNED".equals(r.getStatus())).count());
        stats.put("scheduled", requests.stream().filter(r -> "SCHEDULED".equals(r.getStatus())).count());
        stats.put("resolved", requests.stream().filter(r -> "RESOLVED".equals(r.getStatus())).count());

        stats.put("avgResolutionTime", "2.5 ore");

        return stats;
    }
}

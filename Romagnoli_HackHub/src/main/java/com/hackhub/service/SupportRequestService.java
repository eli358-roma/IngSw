package com.hackhub.service;

import com.hackhub.model.SupportRequest;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.repository.SupportRequestRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //Ottiene tutte le richieste per un hackathon
    public List<SupportRequest> getRequestsByHackathon(Long hackathonId) {
        return supportRequestRepository.findByTeamHackathonId(hackathonId);
    }

    //Programma una call con il mentore
    @Autowired
    private com.hackhub.pattern.facade.CalendarService calendarService;

    public SupportRequest scheduleCall(Long requestId, LocalDateTime scheduledDate, String calendarEventId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Richiesta non trovata"));

        if (request.getMentor() == null) {
            throw new RuntimeException("Nessun mentore assegnato a questa richiesta");
        }

        request.setScheduledDate(scheduledDate);
        request.setCalendarEventId(calendarEventId);
        request.setStatus("SCHEDULED");

        // Invia notifica al team
        String message = String.format(
                "La tua richiesta di supporto '%s' è stata programmata per il %s",
                request.getTitle(),
                scheduledDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );

        // Usa il team per inviare notifica a tutti i membri
        if (request.getTeam() != null && request.getTeam().getMembers() != null) {
            for (User member : request.getTeam().getMembers()) {
                // Qui potresti usare NotificationContext se lo hai injectato
                System.out.println("🔔 Notifica a " + member.getUsername() + ": " + message);
            }
        }

        return supportRequestRepository.save(request);
    }

    /**
     * Chiudi una richiesta
     */
    public SupportRequest closeRequest(Long requestId, String resolution) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Richiesta non trovata"));

        request.setStatus("RESOLVED");
        request.setDescription(request.getDescription() + "\n\nRisoluzione: " + resolution);

        return supportRequestRepository.save(request);
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

        // Tempo medio di risoluzione (simulato)
        stats.put("avgResolutionTime", "2.5 ore");

        return stats;
    }
}

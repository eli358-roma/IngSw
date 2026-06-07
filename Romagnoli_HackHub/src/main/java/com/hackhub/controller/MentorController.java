package com.hackhub.controller;

import com.hackhub.model.SupportRequest;
import com.hackhub.model.Hackathon;
import com.hackhub.service.SupportRequestService;
import com.hackhub.service.HackathonService;
import com.hackhub.pattern.facade.ExternalServiceFacade;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mentor")
public class MentorController {

    @Autowired
    private SupportRequestService supportRequestService;

    @Autowired
    private HackathonService hackathonService;


    @GetMapping("/dashboard")
    public String mentorDashboard(HttpSession session, Model model) {
        Long mentorId = (Long) session.getAttribute("userId");

        if (mentorId == null) {
            return "redirect:/login";
        }

        // Trova gli hackathon dove questo utente è mentore
        List<Hackathon> hackathons = hackathonService.getAllHackathons().stream()
                .filter(h -> h.getMentors().stream().anyMatch(m -> m.getId().equals(mentorId)))
                .collect(Collectors.toList());

        // Richieste assegnate a questo mentore
        List<SupportRequest> myRequests = supportRequestService.getRequestsByMentor(mentorId);

        // Richieste in attesa
        List<SupportRequest> pendingRequests = hackathons.stream()
                .flatMap(h -> supportRequestService.getUnassignedRequestsByHackathon(h.getId()).stream())
                .collect(Collectors.toList());

        model.addAttribute("hackathons", hackathons);
        model.addAttribute("myRequests", myRequests);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("totalPending", pendingRequests.size());

        return "mentor/dashboard";
    }

    @GetMapping("/hackathon/{hackathonId}")
    public String viewHackathon(@PathVariable Long hackathonId,
                                HttpSession session,
                                Model model) {
        Long mentorId = (Long) session.getAttribute("userId");
        Hackathon hackathon = hackathonService.getHackathonById(hackathonId);

        // Verifica che il mentore sia assegnato a questo hackathon
        if (hackathon.getMentors().stream().noneMatch(m -> m.getId().equals(mentorId))) {
            return "redirect:/mentor/dashboard?error=not_authorized";
        }

        List<SupportRequest> allRequests = supportRequestService.getRequestsByHackathon(hackathonId);

        List<SupportRequest> unassigned = allRequests.stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());

        List<SupportRequest> myAssigned = allRequests.stream()
                .filter(r -> r.getMentor() != null && r.getMentor().getId().equals(mentorId))
                .collect(Collectors.toList());

        List<SupportRequest> scheduled = myAssigned.stream()
                .filter(r -> "SCHEDULED".equals(r.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("hackathon", hackathon);
        model.addAttribute("unassigned", unassigned);
        model.addAttribute("myAssigned", myAssigned);
        model.addAttribute("scheduled", scheduled);

        return "mentor/hackathon";
    }

    @PostMapping("/requests/assign")
    public String assignRequest(@RequestParam Long requestId,
                                @RequestParam Long hackathonId,
                                HttpSession session) {
        try {
            Long mentorId = (Long) session.getAttribute("userId");
            supportRequestService.assignMentor(requestId, mentorId);
            return "redirect:/mentor/hackathon/" + hackathonId + "?success=assigned";
        } catch (Exception e) {
            return "redirect:/mentor/hackathon/" + hackathonId + "?error=" + e.getMessage();
        }
    }

    @PostMapping("/requests/schedule")
    public String scheduleCall(@RequestParam Long requestId,
                               @RequestParam String scheduledDate,
                               @RequestParam String scheduledTime,
                               @RequestParam Long hackathonId,
                               HttpSession session) {
        try {
            Long mentorId = (Long) session.getAttribute("userId");

            LocalDateTime dateTime = LocalDateTime.parse(
                    scheduledDate + "T" + scheduledTime + ":00"
            );

            supportRequestService.scheduleCall(requestId, dateTime, mentorId);

            return "redirect:/mentor/hackathon/" + hackathonId + "?success=scheduled";

        } catch (Exception e) {
            return "redirect:/mentor/hackathon/" + hackathonId + "?error=" + e.getMessage();
        }
    }

    @GetMapping("/mentor")
    public String redirectToMentorDashboard() {
        return "redirect:/mentor/dashboard";
    }
}
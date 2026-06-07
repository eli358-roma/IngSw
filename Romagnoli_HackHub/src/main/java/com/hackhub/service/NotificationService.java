package com.hackhub.service;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servizio centralizzato per le notifiche
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.inapp.enabled:true}")
    private boolean inAppEnabled;

    /**
     * Metodo principale per inviare notifiche
     */
    public void sendNotification(String type, String message, User user) {
        if (user == null) {
            log.warn("Tentativo di inviare notifica a utente null");
            return;
        }

        if ("EMAIL".equalsIgnoreCase(type)) {
            sendEmail(user.getEmail(), "Notifica HackHub", message);
        } else if ("IN_APP".equalsIgnoreCase(type)) {
            sendInApp(user.getId(), message);
        } else {
            sendEmail(user.getEmail(), "Notifica HackHub", message);
            sendInApp(user.getId(), message);
        }
    }

    /**
     * Invia una email
     */
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("[EMAIL DISABILITATA] A: {}, Oggetto: {}", to, subject);
            return;
        }

        log.info("EMAIL INVIATA -> A: {}, Oggetto: {}", to, subject);
        log.info("   Corpo: {}", body);
    }

    /**
     * Invia una notifica in-app
     */
    public void sendInApp(Long userId, String message) {
        if (!inAppEnabled) {
            log.info("🔔 [IN-APP DISABILITATA] User: {}, Messaggio: {}", userId, message);
            return;
        }

        log.info("🔔 NOTIFICA IN-APP -> User {}: {}", userId, message);
    }

    /**
     * Invia notifica a tutti i membri di un team
     */
    public void sendToTeam(Team team, String subject, String message) {
        if (team == null || team.getMembers() == null) return;

        for (User member : team.getMembers()) {
            sendEmail(member.getEmail(), subject, message);
            sendInApp(member.getId(), message);
        }
    }

    /**
     * Notifica cambio stato hackathon
     */
    public void notifyHackathonStatusChange(Hackathon hackathon, String oldStatus, String newStatus) {
        String subject = "[" + hackathon.getName() + "] Aggiornamento stato";
        String message = "L'hackathon '" + hackathon.getName() + "' è passato da " +
                getStatusItalian(oldStatus) + " a " + getStatusItalian(newStatus);

        //notifica organizzatore
        if (hackathon.getOrganizer() != null) {
            sendEmail(hackathon.getOrganizer().getEmail(), subject, message);
        }

        //notifica giudice
        if (hackathon.getJudge() != null) {
            sendEmail(hackathon.getJudge().getEmail(), subject, message);
        }

        //notifica mentori
        if (hackathon.getMentors() != null) {
            for (User mentor : hackathon.getMentors()) {
                sendEmail(mentor.getEmail(), subject, message);
                sendInApp(mentor.getId(), message);
            }
        }

        //notifica i partecipanti
        if (hackathon.getTeams() != null) {
            for (Team team : hackathon.getTeams()) {
                sendToTeam(team, subject, message);
            }
        }

        log.info("Notifica cambio stato inviata per: {}", hackathon.getName());
    }

    /**
     * Notifica team valutato
     */
    public void notifyTeamEvaluated(Team team, Double score, String feedback) {
        String subject = "Valutazione team: " + team.getName();
        String message = String.format(
                "Il tuo team '%s' è stato valutato!\nPunteggio: %.1f/10\nFeedback: %s",
                team.getName(), score, feedback != null ? feedback : "Nessun feedback"
        );
        sendToTeam(team, subject, message);
    }

    /**
     * Notifica team vincitore
     */
    public void notifyWinner(Team winningTeam, Hackathon hackathon, Double prizeMoney) {
        String subject = "🏆 CONGRATULAZIONI! Hai vito l'hackathon! 🏆";
        String message = String.format(
                "CONGRATULAZIONI! Il tuo team '%s' ha vinto l'hackathon '%s'!\nPremio: €%.2f",
                winningTeam.getName(), hackathon.getName(), prizeMoney != null ? prizeMoney : 0
        );
        sendToTeam(winningTeam, subject, message);
    }

    /**
     * Notifica nuovo mentore assegnato
     */
    public void notifyMentorAssigned(User mentor, Hackathon hackathon) {
        String subject = "Sei stato assegnato come mentore";
        String message = "Sei stato assegnato come mentore per l'hackathon '" + hackathon.getName() + "'";
        sendEmail(mentor.getEmail(), subject, message);
        sendInApp(mentor.getId(), message);
    }

    /**
     * Notifica nuovo giudice assegnato
     */
    public void notifyJudgeAssigned(User judge, Hackathon hackathon) {
        String subject = "Sei stato assegnato come giudice";
        String message = "Sei stato assegnato come giudice per l'hackathon '" + hackathon.getName() + "'";
        sendEmail(judge.getEmail(), subject, message);
        sendInApp(judge.getId(), message);
    }

    private String getStatusItalian(String status) {
        return switch (status) {
            case "INSCRIZIONE" -> "aperto alle iscrizioni";
            case "IN_CORSO" -> "in corso";
            case "IN_VALUTAZIONE" -> "in fase di valutazione";
            case "CONCLUSO" -> "concluso";
            default -> status;
        };
    }
}
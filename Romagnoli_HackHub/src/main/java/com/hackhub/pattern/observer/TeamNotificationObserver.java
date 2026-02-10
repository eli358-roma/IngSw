package com.hackhub.pattern.observer;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.pattern.strategy.NotificationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TeamNotificationObserver implements HackathonObserver {

    @Autowired
    private NotificationContext notificationContext;

    @Override
    public void onStatusChange(Hackathon hackathon, String oldStatus, String newStatus) {
        String message = String.format(
                "Lo stato dell'hackathon '%s' è cambiato da %s a %s",
                hackathon.getName(), oldStatus, newStatus
        );

        // Notifica tutti i team partecipanti
        for (Team team : hackathon.getTeams()) {
            team.getMembers().forEach(member -> {
                notificationContext.sendNotification("IN_APP", message, member);
            });
        }
    }

    @Override
    public void onJudgeAssigned(Hackathon hackathon) {
        if (hackathon.getJudge() != null) {
            String message = String.format(
                    "Il giudice %s è stato assegnato all'hackathon '%s'",
                    hackathon.getJudge().getUsername(),
                    hackathon.getName()
            );

            // Notifica l'organizzatore
            notificationContext.sendNotification("EMAIL", message, hackathon.getOrganizer());
        }
    }

    @Override
    public void onWinnerDeclared(Hackathon hackathon, Long winnerTeamId) {
        String message = String.format(
                "L'hackathon '%s' ha un vincitore! Team ID: %d",
                hackathon.getName(), winnerTeamId
        );

        // Notifica tutti i partecipanti
        hackathon.getTeams().forEach(team -> {
            boolean isWinner = team.getId().equals(winnerTeamId);
            String teamMessage = isWinner
                    ? "🎉 Congratulazioni! Il tuo team ha vinto l'hackathon!"
                    : "Grazie per aver partecipato all'hackathon!";

            team.getMembers().forEach(member -> {
                notificationContext.sendNotification("EMAIL", teamMessage, member);
            });
        });
    }
}
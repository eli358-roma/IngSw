package com.hackhub.pattern.observer;

import com.hackhub.model.Hackathon;
import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.pattern.strategy.NotificationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TeamNotificationObserver implements HackathonObserver {

    @Autowired
    private NotificationContext notificationContext;

    @Override
    public void onStatusChange(Hackathon hackathon, String oldStatus, String newStatus) {
        // Determina il messaggio in base allo stato
        String message = getStatusChangeMessage(hackathon, oldStatus, newStatus);
        String subject = "[" + hackathon.getName() + "] Aggiornamento stato";

        // Raccogli tutti i destinatari
        Set<User> recipients = new HashSet<>();

        // Aggiungi organizzatore
        if (hackathon.getOrganizer() != null) {
            recipients.add(hackathon.getOrganizer());
        }

        // Aggiungi giudice
        if (hackathon.getJudge() != null) {
            recipients.add(hackathon.getJudge());
        }

        // Aggiungi mentori
        if (hackathon.getMentors() != null) {
            recipients.addAll(hackathon.getMentors());
        }

        // Aggiungi membri dei team
        if (hackathon.getTeams() != null) {
            for (Team team : hackathon.getTeams()) {
                if (team.getMembers() != null) {
                    recipients.addAll(team.getMembers());
                }
            }
        }

        // Invia notifiche
        String fullMessage = subject + "\n\n" + message;
        for (User recipient : recipients) {
            // Invia email per organizzatore e giudice, in-app per gli altri
            if (recipient.equals(hackathon.getOrganizer()) || recipient.equals(hackathon.getJudge())) {
                notificationContext.sendNotification("EMAIL", fullMessage, recipient);
            } else {
                notificationContext.sendNotification("IN_APP", fullMessage, recipient);
            }
        }

        System.out.println("Notificato cambio stato a " + recipients.size() + " utenti");
    }

    private String getStatusChangeMessage(Hackathon hackathon, String oldStatus, String newStatus) {
        String statusItalian = switch (newStatus) {
            case "INSCRIZIONE" -> "aperto alle iscrizioni";
            case "IN_CORSO" -> "in corso";
            case "IN_VALUTAZIONE" -> "in fase di valutazione";
            case "CONCLUSO" -> "concluso";
            default -> newStatus;
        };

        return String.format(
                "Ciao!\n\n" +
                        "L'hackathon '%s' è ora %s.\n\n" +
                        "Cosa puoi fare ora:\n%s\n\n" +
                        "Accedi alla piattaforma per maggiori dettagli.\n\n" +
                        "HackHub Team",
                hackathon.getName(),
                statusItalian,
                getNextSteps(newStatus)
        );
    }

    private String getNextSteps(String status) {
        return switch (status) {
            case "INSCRIZIONE" -> "• I team possono registrarsi\n• I mentori possono prendere visione dei team";
            case "IN_CORSO" -> "• I team possono lavorare ai progetti\n• I mentori possono supportare i team\n• Le sottomissioni sono aperte";
            case "IN_VALUTAZIONE" -> "• I giudici valutano i progetti\n• I team attendono i risultati";
            case "CONCLUSO" -> "• I vincitori vengono proclamati\n• I premi vengono erogati";
            default -> "• Controlla la piattaforma per aggiornamenti";
        };
    }

    @Override
    public void onJudgeAssigned(Hackathon hackathon) {
        if (hackathon.getJudge() == null) return;

        String message = String.format(
                "Sei stato assegnato come giudice per l'hackathon '%s'.\n\n" +
                        "Quando l'hackathon entrerà in fase di valutazione, potrai:\n" +
                        "• Visualizzare tutte le sottomissioni\n" +
                        "• Assegnare un punteggio (0-10) a ciascun progetto\n" +
                        "• Lasciare un feedback scritto\n\n" +
                        "Grazie per la tua disponibilità!",
                hackathon.getName()
        );

        notificationContext.sendNotification("EMAIL", message, hackathon.getJudge());
    }

    @Override
    public void onWinnerDeclared(Hackathon hackathon, Long winnerTeamId) {
        if (hackathon.getTeams() == null) return;

        // Trova il team vincitore
        Team winnerTeam = hackathon.getTeams().stream()
                .filter(t -> t.getId().equals(winnerTeamId))
                .findFirst()
                .orElse(null);

        for (Team team : hackathon.getTeams()) {
            boolean isWinner = team.getId().equals(winnerTeamId);
            String message;

            if (isWinner) {
                message = String.format(
                        "CONGRATULAZIONI! Il tuo team '%s' ha vinto l'hackathon '%s'!\n\n" +
                                "Il premio di €%s verrà erogato al team leader.\n\n" +
                                "Siamo orgogliosi del vostro lavoro! Continuate così!",
                        team.getName(),
                        hackathon.getName(),
                        hackathon.getPrizeMoney() != null ? hackathon.getPrizeMoney() : "0"
                );
            } else {
                message = String.format(
                        "Grazie per aver partecipato all'hackathon '%s' con il team '%s'.\n\n" +
                                "Il team vincitore è: %s\n\n" +
                                "Non mollare! Ci vediamo al prossimo hackathon!",
                        hackathon.getName(),
                        team.getName(),
                        winnerTeam != null ? winnerTeam.getName() : "Team " + winnerTeamId
                );
            }

            // Invia a tutti i membri del team
            if (team.getMembers() != null) {
                for (User member : team.getMembers()) {
                    notificationContext.sendNotification("EMAIL", message, member);
                }
            }
        }
    }
}

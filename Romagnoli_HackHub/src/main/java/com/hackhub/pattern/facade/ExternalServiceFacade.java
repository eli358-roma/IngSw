package com.hackhub.pattern.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Design Pattern: FACADE
 * Fornisce un'interfaccia semplificata per i servizi esterni (Calendar, Payment)
 * Nasconde la complessità dei sottosistemi e fornisce metodi di alto livello
 * come scheduleMentorCall(), processPrizePayment(), processWinnerServices()
 */
@Service
public class ExternalServiceFacade {

    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceFacade.class);

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private PaymentService paymentService;

    /**
     * Facade method: Semplifica la prenotazione di una call mentore-team
     */
    public String scheduleMentorCall(String mentorName, String mentorEmail,
                                     String teamName, String teamLeaderEmail,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     String topic) {

        logger.info("Scheduling mentor call: {} with {}", mentorName, teamName);

        String title = String.format("Support Call: %s - %s", teamName, topic);
        String description = String.format("Mentoring session for team %s with mentor %s. Topic: %s",
                teamName, mentorName, topic);

        CalendarService.CalendarEvent event = calendarService.scheduleMeeting(
                title, description, startTime, endTime, mentorEmail, teamLeaderEmail);

        logger.info("Call scheduled: {}", event.getId());
        return event.getId();
    }

    /**
     * Facade method: Semplifica il pagamento del premio
     */
    public String processPrizePayment(BigDecimal amount, String currency,
                                      String teamName, String teamLeaderEmail,
                                      String teamLeaderName, String hackathonName) {

        logger.info("Processing prize payment: {} for {}", amount, teamName);

        PaymentService.PaymentTransaction transaction = paymentService.processPrizePayment(
                amount, currency, teamName, teamLeaderEmail, teamLeaderName, hackathonName);

        logger.info("Payment processed: {}", transaction.getId());
        return transaction.getId();
    }

    /**
     * Versione con valuta di default (EUR)
     */
    public String processPrizePayment(BigDecimal amount, String teamName,
                                      String teamLeaderEmail, String teamLeaderName,
                                      String hackathonName) {
        return processPrizePayment(amount, "EUR", teamName, teamLeaderEmail, teamLeaderName, hackathonName);
    }

    /**
     * Facade method: Servizio completo per vincitore
     */
    public WinnerServices processWinnerServices(String hackathonName, String winningTeamName,
                                                String teamLeaderEmail, String teamLeaderName,
                                                BigDecimal prizeAmount, String mentorEmail,
                                                LocalDateTime awardCeremonyTime) {

        logger.info("Processing complete winner services for: {}", winningTeamName);

        WinnerServices result = new WinnerServices();
        result.setHackathonName(hackathonName);
        result.setWinningTeamName(winningTeamName);
        result.setProcessedAt(LocalDateTime.now());

        try {
            //processa pagamento
            String paymentId = processPrizePayment(prizeAmount, winningTeamName,
                    teamLeaderEmail, teamLeaderName, hackathonName);
            result.setPaymentTransactionId(paymentId);
            result.setPaymentAmount(prizeAmount);
            result.setPaymentStatus("COMPLETED");

            //programma call di congratulazioni
            LocalDateTime endTime = awardCeremonyTime.plusHours(1);
            String callId = scheduleMentorCall(
                    "Award Coordinator", mentorEmail,
                    winningTeamName, teamLeaderEmail,
                    awardCeremonyTime, endTime,
                    "Premiazione e Congratulazioni"
            );
            result.setCelebrationCallId(callId);
            result.setCelebrationCallTime(awardCeremonyTime);

            //programma call di follow-up
            LocalDateTime followUpTime = awardCeremonyTime.plusDays(7);
            LocalDateTime followUpEndTime = followUpTime.plusHours(1);
            String followUpId = scheduleMentorCall(
                    "Follow-up Mentor", mentorEmail,
                    winningTeamName, teamLeaderEmail,
                    followUpTime, followUpEndTime,
                    "Follow-up e Prossimi Passi"
            );
            result.setFollowUpCallId(followUpId);
            result.setFollowUpCallTime(followUpTime);

            result.setSuccess(true);
            result.setMessage("Tutti i servizi elaborati con successo");

            logger.info("Winner services completed successfully for: {}", winningTeamName);

        } catch (Exception e) {
            logger.error("Error processing winner services", e);
            result.setSuccess(false);
            result.setMessage("Errore: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Cancella una call programmata
     */
    public boolean cancelScheduledCall(String callId) {
        logger.info("Cancelling call: {}", callId);
        try {
            boolean cancelled = calendarService.cancelMeeting(callId);
            if (cancelled) {
                logger.info("Call cancelled: {}", callId);
            } else {
                logger.warn("Failed to cancel call: {}", callId);
            }
            return cancelled;
        } catch (Exception e) {
            logger.error("Error cancelling call", e);
            return false;
        }
    }

    /**
     * Verifica lo stato di un pagamento
     */
    public String checkPaymentStatus(String transactionId) {
        try {
            PaymentService.PaymentTransaction transaction = paymentService.getTransactionStatus(transactionId);
            return transaction.getStatus();
        } catch (Exception e) {
            logger.error("Error checking payment status", e);
            return "ERROR";
        }
    }

    /**
     * Ottieni dettagli di una call
     */
    public CalendarService.CalendarEvent getCallDetails(String callId) {
        return calendarService.getEvent(callId);
    }

    /**
     * Classe per raggruppare i risultati dei servizi per il vincitore
     */
    public static class WinnerServices {
        private String hackathonName;
        private String winningTeamName;
        private String paymentTransactionId;
        private BigDecimal paymentAmount;
        private String paymentStatus;
        private String celebrationCallId;
        private LocalDateTime celebrationCallTime;
        private String followUpCallId;
        private LocalDateTime followUpCallTime;
        private boolean success;
        private String message;
        private String errorMessage;
        private LocalDateTime processedAt;

        public WinnerServices() {}

        public WinnerServices(String hackathonName, String winningTeamName) {
            this.hackathonName = hackathonName;
            this.winningTeamName = winningTeamName;
            this.processedAt = LocalDateTime.now();
        }

        public String getHackathonName() {
            return hackathonName;
        }

        public void setHackathonName(String hackathonName) {
            this.hackathonName = hackathonName;
        }

        public String getWinningTeamName() {
            return winningTeamName;
        }

        public void setWinningTeamName(String winningTeamName) {
            this.winningTeamName = winningTeamName;
        }

        public String getPaymentTransactionId() {
            return paymentTransactionId;
        }

        public void setPaymentTransactionId(String paymentTransactionId) {
            this.paymentTransactionId = paymentTransactionId;
        }

        public BigDecimal getPaymentAmount() {
            return paymentAmount;
        }

        public void setPaymentAmount(BigDecimal paymentAmount) {
            this.paymentAmount = paymentAmount;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public void setPaymentStatus(String paymentStatus) {
            this.paymentStatus = paymentStatus;
        }

        public String getCelebrationCallId() {
            return celebrationCallId;
        }

        public void setCelebrationCallId(String celebrationCallId) {
            this.celebrationCallId = celebrationCallId;
        }

        public LocalDateTime getCelebrationCallTime() {
            return celebrationCallTime;
        }

        public void setCelebrationCallTime(LocalDateTime celebrationCallTime) {
            this.celebrationCallTime = celebrationCallTime;
        }

        public String getFollowUpCallId() {
            return followUpCallId;
        }

        public void setFollowUpCallId(String followUpCallId) {
            this.followUpCallId = followUpCallId;
        }

        public LocalDateTime getFollowUpCallTime() {
            return followUpCallTime;
        }

        public void setFollowUpCallTime(LocalDateTime followUpCallTime) {
            this.followUpCallTime = followUpCallTime;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public LocalDateTime getProcessedAt() {
            return processedAt;
        }

        public void setProcessedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
        }

        public String getFormattedCelebrationTime() {
            if (celebrationCallTime == null) return "Non programmata";
            return celebrationCallTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        public String getFormattedFollowUpTime() {
            if (followUpCallTime == null) return "Non programmata";
            return followUpCallTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        public String getFormattedPaymentAmount() {
            if (paymentAmount == null) return "€0.00";
            return String.format("€%.2f", paymentAmount);
        }

        @Override
        public String toString() {
            return String.format("WinnerServices[hackathon=%s, team=%s, success=%s, paymentId=%s]",
                    hackathonName, winningTeamName, success, paymentTransactionId);
        }
    }
}

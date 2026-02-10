package com.hackhub.pattern.facade;

import com.hackhub.pattern.facade.CalendarService;
import com.hackhub.pattern.facade.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Facade per semplificare l'uso dei servizi esterni
 * Implementa il design pattern Facade
 */
@Service
public class ExternalServiceFacade {

    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceFacade.class);

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private PaymentService paymentService;

    /**
     * Prenota una call tra mentore e team
     * Facade method che semplifica l'interazione con CalendarService
     */
    public String scheduleMentorCall(String mentorName, String mentorEmail,
                                     String teamName, String teamLeaderEmail,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     String topic) {

        logger.info("Scheduling mentor call via facade: {} with {}", mentorName, teamName);

        String title = String.format("Support Call: %s - %s", teamName, topic);
        String description = String.format("Mentoring session for team %s with mentor %s. Topic: %s",
                teamName, mentorName, topic);

        try {
            CalendarService.CalendarEvent event = calendarService.scheduleMeeting(
                    title, description, startTime, endTime, mentorEmail, teamLeaderEmail);

            logger.info("Call scheduled successfully: {}", event.getId());
            return event.getId();

        } catch (Exception e) {
            logger.error("Failed to schedule mentor call", e);
            throw new RuntimeException("Failed to schedule call: " + e.getMessage(), e);
        }
    }

    /**
     * Prenota una serie di call ricorrenti
     */
    public String[] scheduleRecurringMentorCalls(String mentorName, String mentorEmail,
                                                 String teamName, String teamLeaderEmail,
                                                 LocalDateTime firstCallTime,
                                                 int numberOfCalls, int daysBetweenCalls) {

        logger.info("Scheduling {} recurring calls for team: {}", numberOfCalls, teamName);

        String[] eventIds = new String[numberOfCalls];
        LocalDateTime currentCallTime = firstCallTime;

        for (int i = 0; i < numberOfCalls; i++) {
            String topic = String.format("Session %d/%d", i + 1, numberOfCalls);

            LocalDateTime endTime = currentCallTime.plusHours(1); // Call di 1 ora

            String eventId = scheduleMentorCall(
                    mentorName, mentorEmail,
                    teamName, teamLeaderEmail,
                    currentCallTime, endTime,
                    topic
            );

            eventIds[i] = eventId;
            currentCallTime = currentCallTime.plusDays(daysBetweenCalls);
        }

        logger.info("Scheduled {} recurring calls successfully", numberOfCalls);
        return eventIds;
    }

    /**
     * Cancella una call programmata
     */
    public boolean cancelMentorCall(String eventId) {
        logger.info("Cancelling mentor call: {}", eventId);

        try {
            boolean success = calendarService.cancelMeeting(eventId);

            if (success) {
                logger.info("Call cancelled successfully: {}", eventId);
            } else {
                logger.warn("Failed to cancel call: {}", eventId);
            }

            return success;

        } catch (Exception e) {
            logger.error("Error cancelling call", e);
            return false;
        }
    }

    /**
     * Processa il pagamento del premio al team vincitore
     * Facade method che semplifica l'interazione con PaymentService
     */
    public String processHackathonPrize(BigDecimal amount, String currency,
                                        String teamName, String teamLeaderEmail,
                                        String teamLeaderName, String hackathonName) {

        logger.info("Processing prize payment via facade: {} for {}", amount, teamName);

        try {
            PaymentService.PaymentTransaction transaction = paymentService.processPrizePayment(
                    amount, currency, teamName, teamLeaderEmail, teamLeaderName, hackathonName
            );

            logger.info("Prize payment processed successfully: {}", transaction.getId());
            return transaction.getId();

        } catch (Exception e) {
            logger.error("Failed to process prize payment", e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage(), e);
        }
    }

    /**
     * Processa il pagamento del premio con valori di default
     */
    public String processHackathonPrize(BigDecimal amount,
                                        String teamName, String teamLeaderEmail,
                                        String teamLeaderName, String hackathonName) {

        return processHackathonPrize(amount, "EUR", teamName, teamLeaderEmail,
                teamLeaderName, hackathonName);
    }

    /**
     * Verifica lo stato del pagamento
     */
    public String checkPaymentStatus(String transactionId) {
        logger.debug("Checking payment status: {}", transactionId);

        try {
            PaymentService.PaymentTransaction transaction =
                    paymentService.getTransactionStatus(transactionId);

            return transaction.getStatus();

        } catch (Exception e) {
            logger.error("Error checking payment status", e);
            return "ERROR";
        }
    }

    /**
     * Rimborsa un pagamento
     */
    public String refundPrizePayment(String transactionId, String reason) {
        logger.info("Refunding payment: {} - Reason: {}", transactionId, reason);

        try {
            PaymentService.PaymentTransaction refund =
                    paymentService.refundPayment(transactionId, reason);

            logger.info("Payment refunded successfully: {}", refund.getId());
            return refund.getId();

        } catch (Exception e) {
            logger.error("Failed to refund payment", e);
            throw new RuntimeException("Failed to refund: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica la disponibilità del mentore
     */
    public boolean checkMentorAvailability(String mentorEmail,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime) {

        logger.debug("Checking mentor availability: {}", mentorEmail);

        try {
            return calendarService.isMentorAvailable(mentorEmail, startTime, endTime);

        } catch (Exception e) {
            logger.error("Error checking mentor availability", e);
            return false;
        }
    }

    /**
     * Ottieni informazioni su una call programmata
     */
    public CalendarService.CalendarEvent getScheduledCallDetails(String eventId) {
        logger.debug("Getting call details: {}", eventId);

        try {
            return calendarService.getEvent(eventId);

        } catch (Exception e) {
            logger.error("Error getting call details", e);
            throw new RuntimeException("Call not found: " + eventId, e);
        }
    }

    /**
     * Servizio completo per organizzatore: programma call e gestisce pagamenti
     */
    public HackathonExternalServices processWinnerServices(String hackathonName,
                                                           String winningTeamName,
                                                           String teamLeaderEmail,
                                                           String teamLeaderName,
                                                           BigDecimal prizeAmount,
                                                           String mentorEmail,
                                                           LocalDateTime awardCeremonyTime) {

        logger.info("Processing complete external services for hackathon winner: {}", hackathonName);

        HackathonExternalServices result = new HackathonExternalServices();
        result.setHackathonName(hackathonName);
        result.setWinningTeamName(winningTeamName);

        try {
            // 1. Processa il pagamento del premio
            String paymentId = processHackathonPrize(
                    prizeAmount, "EUR", winningTeamName,
                    teamLeaderEmail, teamLeaderName, hackathonName
            );
            result.setPaymentTransactionId(paymentId);

            // 2. Programma una call di congratulazioni con il mentore
            LocalDateTime endTime = awardCeremonyTime.plusHours(1);
            String callId = scheduleMentorCall(
                    "Award Mentor", mentorEmail,
                    winningTeamName, teamLeaderEmail,
                    awardCeremonyTime, endTime,
                    "Prize Award and Congratulations"
            );
            result.setCelebrationCallId(callId);

            // 3. Programma una sessione di follow-up
            LocalDateTime followUpTime = awardCeremonyTime.plusDays(7);
            LocalDateTime followUpEndTime = followUpTime.plusHours(1);
            String followUpId = scheduleMentorCall(
                    "Follow-up Mentor", mentorEmail,
                    winningTeamName, teamLeaderEmail,
                    followUpTime, followUpEndTime,
                    "Project Follow-up and Next Steps"
            );
            result.setFollowUpCallId(followUpId);

            result.setSuccess(true);
            result.setMessage("All external services processed successfully");

            logger.info("Complete external services processed for winner: {}", winningTeamName);

        } catch (Exception e) {
            logger.error("Failed to process external services for winner", e);
            result.setSuccess(false);
            result.setMessage("Failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Classe per raggruppare i risultati dei servizi esterni
     */
    public static class HackathonExternalServices {
        private String hackathonName;
        private String winningTeamName;
        private String paymentTransactionId;
        private String celebrationCallId;
        private String followUpCallId;
        private boolean success;
        private String message;

        // Getter e Setter
        public String getHackathonName() { return hackathonName; }
        public void setHackathonName(String hackathonName) { this.hackathonName = hackathonName; }

        public String getWinningTeamName() { return winningTeamName; }
        public void setWinningTeamName(String winningTeamName) { this.winningTeamName = winningTeamName; }

        public String getPaymentTransactionId() { return paymentTransactionId; }
        public void setPaymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }

        public String getCelebrationCallId() { return celebrationCallId; }
        public void setCelebrationCallId(String celebrationCallId) { this.celebrationCallId = celebrationCallId; }

        public String getFollowUpCallId() { return followUpCallId; }
        public void setFollowUpCallId(String followUpCallId) { this.followUpCallId = followUpCallId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        @Override
        public String toString() {
            return String.format("HackathonExternalServices[hackathon=%s, team=%s, success=%s]",
                    hackathonName, winningTeamName, success);
        }
    }

    /**
     * Verifica lo stato di tutti i servizi esterni
     */
    public ServiceStatus checkAllServicesStatus() {
        ServiceStatus status = new ServiceStatus();
        status.setCheckedAt(LocalDateTime.now());

        try {
            // Test Calendar Service
            CalendarService.CalendarEvent testEvent = calendarService.scheduleMeeting(
                    "Test", "Test event",
                    LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1),
                    "test@hackhub.com", "test@team.com"
            );
            status.setCalendarServiceAvailable(true);
            calendarService.cancelMeeting(testEvent.getId());

        } catch (Exception e) {
            status.setCalendarServiceAvailable(false);
            status.setCalendarServiceError(e.getMessage());
        }

        try {
            // Test Payment Service
            PaymentService.PaymentTransaction testTx = paymentService.processPrizePayment(
                    new BigDecimal("1.00"), "EUR", "Test Team",
                    "test@team.com", "Test Leader", "Test Hackathon"
            );
            status.setPaymentServiceAvailable(true);
            status.setTestTransactionId(testTx.getId());

        } catch (Exception e) {
            status.setPaymentServiceAvailable(false);
            status.setPaymentServiceError(e.getMessage());
        }

        status.setAllServicesAvailable(
                status.isCalendarServiceAvailable() &&
                        status.isPaymentServiceAvailable()
        );

        return status;
    }

    /**
     * Classe per lo stato dei servizi
     */
    public static class ServiceStatus {
        private LocalDateTime checkedAt;
        private boolean calendarServiceAvailable;
        private String calendarServiceError;
        private boolean paymentServiceAvailable;
        private String paymentServiceError;
        private boolean allServicesAvailable;
        private String testTransactionId;

        // Getter e Setter
        public LocalDateTime getCheckedAt() { return checkedAt; }
        public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

        public boolean isCalendarServiceAvailable() { return calendarServiceAvailable; }
        public void setCalendarServiceAvailable(boolean calendarServiceAvailable) { this.calendarServiceAvailable = calendarServiceAvailable; }

        public String getCalendarServiceError() { return calendarServiceError; }
        public void setCalendarServiceError(String calendarServiceError) { this.calendarServiceError = calendarServiceError; }

        public boolean isPaymentServiceAvailable() { return paymentServiceAvailable; }
        public void setPaymentServiceAvailable(boolean paymentServiceAvailable) { this.paymentServiceAvailable = paymentServiceAvailable; }

        public String getPaymentServiceError() { return paymentServiceError; }
        public void setPaymentServiceError(String paymentServiceError) { this.paymentServiceError = paymentServiceError; }

        public boolean isAllServicesAvailable() { return allServicesAvailable; }
        public void setAllServicesAvailable(boolean allServicesAvailable) { this.allServicesAvailable = allServicesAvailable; }

        public String getTestTransactionId() { return testTransactionId; }
        public void setTestTransactionId(String testTransactionId) { this.testTransactionId = testTransactionId; }

        @Override
        public String toString() {
            return String.format("ServiceStatus[calendar=%s, payment=%s, all=%s]",
                    calendarServiceAvailable, paymentServiceAvailable, allServicesAvailable);
        }
    }
}
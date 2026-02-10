package com.hackhub.pattern.facade;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servizio per gestire le prenotazioni di call tra mentori e team
 * Simula l'integrazione con un servizio di calendario esterno (Google Calendar, Outlook, etc.)
 */
@Service
public class CalendarService {

    private static final Logger logger = LoggerFactory.getLogger(CalendarService.class);

    @Value("${calendar.service.enabled:true}")
    private boolean enabled;

    @Value("${calendar.service.provider:mock}")
    private String provider;

    // Mappa per tracciare gli eventi creati (simula database esterno)
    private final Map<String, CalendarEvent> events = new HashMap<>();

    /**
     * Crea un nuovo evento nel calendario per una call tra mentore e team
     */
    public CalendarEvent scheduleMeeting(String title, String description,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         String mentorEmail, String teamEmail) {

        if (!enabled) {
            logger.warn("Calendar service is disabled. Returning mock event.");
            return createMockEvent(title, startTime, endTime);
        }

        // Genera ID evento univoco
        String eventId = generateEventId();

        // Crea l'evento
        CalendarEvent event = new CalendarEvent();
        event.setId(eventId);
        event.setTitle(title);
        event.setDescription(description);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setOrganizerEmail(mentorEmail);
        event.addAttendee(teamEmail);
        event.addAttendee(mentorEmail);
        event.setStatus("SCHEDULED");
        event.setCreatedAt(LocalDateTime.now());

        // Simula chiamata API al servizio esterno
        if ("google".equalsIgnoreCase(provider)) {
            return scheduleGoogleCalendarEvent(event);
        } else if ("outlook".equalsIgnoreCase(provider)) {
            return scheduleOutlookEvent(event);
        } else {
            // Mock provider - salva in memoria
            events.put(eventId, event);
            logger.info("Mock calendar event created: {}", eventId);

            // Simula ritardo di rete
            simulateNetworkDelay();

            return event;
        }
    }

    /**
     * Cancella un evento dal calendario
     */
    public boolean cancelMeeting(String eventId) {
        if (!enabled) {
            logger.warn("Calendar service is disabled. Mock cancellation.");
            return true;
        }

        CalendarEvent event = events.get(eventId);
        if (event == null) {
            logger.error("Event not found: {}", eventId);
            return false;
        }

        event.setStatus("CANCELLED");
        event.setCancelledAt(LocalDateTime.now());

        // In produzione, qui chiameresti l'API esterna
        logger.info("Event cancelled: {}", eventId);
        simulateNetworkDelay();

        return true;
    }

    /**
     * Aggiorna un evento esistente
     */
    public CalendarEvent updateMeeting(String eventId, LocalDateTime newStartTime,
                                       LocalDateTime newEndTime) {

        CalendarEvent event = events.get(eventId);
        if (event == null) {
            throw new RuntimeException("Event not found: " + eventId);
        }

        event.setStartTime(newStartTime);
        event.setEndTime(newEndTime);
        event.setUpdatedAt(LocalDateTime.now());

        logger.info("Event updated: {}", eventId);
        simulateNetworkDelay();

        return event;
    }

    /**
     * Ottieni i dettagli di un evento
     */
    public CalendarEvent getEvent(String eventId) {
        CalendarEvent event = events.get(eventId);
        if (event == null) {
            throw new RuntimeException("Event not found: " + eventId);
        }
        return event;
    }

    /**
     * Verifica la disponibilità di un mentore in un dato orario
     */
    public boolean isMentorAvailable(String mentorEmail, LocalDateTime startTime,
                                     LocalDateTime endTime) {
        // Controlla se il mentore ha già eventi in quell'orario
        return events.values().stream()
                .filter(e -> e.getAttendees().contains(mentorEmail))
                .filter(e -> e.getStatus().equals("SCHEDULED"))
                .noneMatch(e -> isTimeOverlapping(e.getStartTime(), e.getEndTime(), startTime, endTime));
    }

    /**
     * Metodo per integrazione con Google Calendar (mock)
     */
    private CalendarEvent scheduleGoogleCalendarEvent(CalendarEvent event) {
        logger.info("Scheduling Google Calendar event for: {}", event.getOrganizerEmail());

        // Simula chiamata API a Google Calendar
        String googleEventId = "google_" + UUID.randomUUID().toString();
        event.setExternalId(googleEventId);
        event.setProvider("Google Calendar");

        events.put(event.getId(), event);
        simulateNetworkDelay(1500); // Simula chiamata più lunga

        logger.info("Google Calendar event created successfully: {}", googleEventId);
        return event;
    }

    /**
     * Metodo per integrazione con Outlook Calendar (mock)
     */
    private CalendarEvent scheduleOutlookEvent(CalendarEvent event) {
        logger.info("Scheduling Outlook Calendar event for: {}", event.getOrganizerEmail());

        // Simula chiamata API a Outlook
        String outlookEventId = "outlook_" + UUID.randomUUID().toString();
        event.setExternalId(outlookEventId);
        event.setProvider("Outlook Calendar");

        events.put(event.getId(), event);
        simulateNetworkDelay(1200);

        logger.info("Outlook Calendar event created successfully: {}", outlookEventId);
        return event;
    }

    /**
     * Crea un evento mock (per testing o quando il servizio è disabilitato)
     */
    private CalendarEvent createMockEvent(String title, LocalDateTime startTime, LocalDateTime endTime) {
        CalendarEvent event = new CalendarEvent();
        event.setId("mock_" + UUID.randomUUID().toString());
        event.setTitle(title);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setStatus("MOCK");
        event.setProvider("Mock Service");
        event.setCreatedAt(LocalDateTime.now());

        events.put(event.getId(), event);
        return event;
    }

    // ========== METODI DI UTILITÀ ==========

    private String generateEventId() {
        return "event_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isTimeOverlapping(LocalDateTime start1, LocalDateTime end1,
                                      LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private void simulateNetworkDelay() {
        simulateNetworkDelay(800); // Default 800ms
    }

    private void simulateNetworkDelay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Classe interna per rappresentare un evento del calendario
     */
    public static class CalendarEvent {
        private String id;
        private String externalId;
        private String title;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String organizerEmail;
        private List<String> attendees = new ArrayList<>();
        private String status;
        private String provider;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime cancelledAt;

        // Getter e Setter
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

        public String getOrganizerEmail() { return organizerEmail; }
        public void setOrganizerEmail(String organizerEmail) { this.organizerEmail = organizerEmail; }

        public List<String> getAttendees() { return attendees; }
        public void setAttendees(List<String> attendees) { this.attendees = attendees; }
        public void addAttendee(String email) { this.attendees.add(email); }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public LocalDateTime getCancelledAt() { return cancelledAt; }
        public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return String.format("CalendarEvent[id=%s, title='%s', start=%s, status=%s]",
                    id, title, startTime.format(formatter), status);
        }
    }
}
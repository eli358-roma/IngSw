package com.hackhub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_requests")
public class SupportRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime requestDate;
    private String status; // "PENDING", "ASSIGNED", "SCHEDULED", "RESOLVED"
    private String calendarEventId; // ID dell'evento nel calendario esterno
    private LocalDateTime scheduledDate;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "mentor_id")
    private User mentor;

    // Costruttori
    public SupportRequest() {
        this.requestDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    public SupportRequest(String title, String description, Team team) {
        this();
        this.title = title;
        this.description = description;
        this.team = team;
    }

    public SupportRequest(String title, String description, Team team, User mentor) {
        this(title, description, team);
        this.mentor = mentor;
        if (mentor != null) {
            this.status = "ASSIGNED";
        }
    }

    // Metodi utili
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isAssigned() {
        return "ASSIGNED".equals(status) || "SCHEDULED".equals(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(status);
    }

    // Getter e Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCalendarEventId() {
        return calendarEventId;
    }

    public void setCalendarEventId(String calendarEventId) {
        this.calendarEventId = calendarEventId;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getMentor() {
        return mentor;
    }

    public void setMentor(User mentor) {
        this.mentor = mentor;
    }

    @Override
    public String toString() {
        return "SupportRequest{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", team=" + (team != null ? team.getName() : "null") +
                ", mentor=" + (mentor != null ? mentor.getUsername() : "null") +
                '}';
    }
}
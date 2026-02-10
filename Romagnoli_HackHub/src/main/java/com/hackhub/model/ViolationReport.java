package com.hackhub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "violation_reports")
public class ViolationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime reportDate;
    private String status; // "PENDING", "UNDER_REVIEW", "RESOLVED", "DISMISSED"
    private String resolutionNotes;
    private LocalDateTime resolutionDate;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private User reporter; // Il mentore che ha segnalato

    @ManyToOne
    @JoinColumn(name = "reported_team_id")
    private Team reportedTeam;

    @ManyToOne
    @JoinColumn(name = "assigned_organizer_id")
    private User assignedOrganizer;

    @ManyToOne
    @JoinColumn(name = "hackathon_id")
    private Hackathon hackathon;

    // Costruttori
    public ViolationReport() {
        this.reportDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    public ViolationReport(String title, String description, User reporter, Team reportedTeam, Hackathon hackathon) {
        this();
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.reportedTeam = reportedTeam;
        this.hackathon = hackathon;
    }

    public ViolationReport(String title, String description, User reporter, Team reportedTeam,
                           Hackathon hackathon, User assignedOrganizer) {
        this(title, description, reporter, reportedTeam, hackathon);
        this.assignedOrganizer = assignedOrganizer;
        if (assignedOrganizer != null) {
            this.status = "UNDER_REVIEW";
        }
    }

    // Metodi utili
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isUnderReview() {
        return "UNDER_REVIEW".equals(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(status) || "DISMISSED".equals(status);
    }

    public void resolve(String resolutionNotes, boolean violationConfirmed) {
        this.resolutionNotes = resolutionNotes;
        this.resolutionDate = LocalDateTime.now();
        this.status = violationConfirmed ? "RESOLVED" : "DISMISSED";
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

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public Team getReportedTeam() {
        return reportedTeam;
    }

    public void setReportedTeam(Team reportedTeam) {
        this.reportedTeam = reportedTeam;
    }

    public User getAssignedOrganizer() {
        return assignedOrganizer;
    }

    public void setAssignedOrganizer(User assignedOrganizer) {
        this.assignedOrganizer = assignedOrganizer;
    }

    public Hackathon getHackathon() {
        return hackathon;
    }

    public void setHackathon(Hackathon hackathon) {
        this.hackathon = hackathon;
    }

    @Override
    public String toString() {
        return "ViolationReport{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", reporter=" + (reporter != null ? reporter.getUsername() : "null") +
                ", reportedTeam=" + (reportedTeam != null ? reportedTeam.getName() : "null") +
                ", organizer=" + (assignedOrganizer != null ? assignedOrganizer.getUsername() : "null") +
                '}';
    }
}
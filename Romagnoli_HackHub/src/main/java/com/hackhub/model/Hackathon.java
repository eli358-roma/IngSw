package com.hackhub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hackathons")
public class Hackathon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 2000)
    private String rules;

    @Column(nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private String status = "INSCRIZIONE"; // "INSCRIZIONE", "IN_CORSO", "IN_VALUTAZIONE", "CONCLUSO"

    @Column(nullable = false)
    private Integer maxTeamSize;

    // ========== RELAZIONI ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    @JsonIgnore
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id")
    @JsonIgnore
    private User judge;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hackathon_mentors",
            joinColumns = @JoinColumn(name = "hackathon_id"),
            inverseJoinColumns = @JoinColumn(name = "mentor_id")
    )
    @JsonIgnore
    private List<User> mentors = new ArrayList<>();

    @OneToMany(mappedBy = "hackathon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Team> teams = new ArrayList<>();

    @Column
    private Long winnerTeamId;

    // ========== COSTRUTTORI ==========

    public Hackathon() {}

    public Hackathon(String name, String description, String rules,
                     LocalDateTime registrationDeadline, LocalDateTime startDate,
                     LocalDateTime endDate, Integer maxTeamSize, User organizer) {
        this.name = name;
        this.description = description;
        this.rules = rules;
        this.registrationDeadline = registrationDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxTeamSize = maxTeamSize;
        this.organizer = organizer;
        this.status = "INSCRIZIONE";
    }

    // ========== METODI DI UTILITÀ ==========

    public boolean isRegistrationOpen() {
        return "INSCRIZIONE".equals(status) &&
                LocalDateTime.now().isBefore(registrationDeadline);
    }

    public boolean isInProgress() {
        return "IN_CORSO".equals(status) &&
                LocalDateTime.now().isAfter(startDate) &&
                LocalDateTime.now().isBefore(endDate);
    }

    // ========== GETTER E SETTER ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getMaxTeamSize() { return maxTeamSize; }
    public void setMaxTeamSize(Integer maxTeamSize) { this.maxTeamSize = maxTeamSize; }

    public User getOrganizer() { return organizer; }
    public void setOrganizer(User organizer) { this.organizer = organizer; }

    public User getJudge() { return judge; }
    public void setJudge(User judge) { this.judge = judge; }

    public List<User> getMentors() { return mentors; }
    public void setMentors(List<User> mentors) { this.mentors = mentors; }

    public List<Team> getTeams() { return teams; }
    public void setTeams(List<Team> teams) { this.teams = teams; }

    public Long getWinnerTeamId() { return winnerTeamId; }
    public void setWinnerTeamId(Long winnerTeamId) { this.winnerTeamId = winnerTeamId; }

    @Override
    public String toString() {
        return "Hackathon{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", teams=" + (teams != null ? teams.size() : 0) +
                '}';
    }
}
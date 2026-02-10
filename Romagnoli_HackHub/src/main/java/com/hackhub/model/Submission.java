package com.hackhub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Submission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;
    private String projectDescription;
    private String repositoryUrl;
    private LocalDateTime submissionDate;
    private boolean isFinal;
    private Double score;  // 0-10
    private String judgeFeedback;
    private User evaluatedBy; // Judge che ha valutato


    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    public Submission() {}

    public Submission(String projectName, String projectDescription, String repositoryUrl, Team team) {
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.repositoryUrl = repositoryUrl;
        this.team = team;
        this.submissionDate = LocalDateTime.now();
        this.isFinal = false;
    }

    // Getter e Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }

    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
}
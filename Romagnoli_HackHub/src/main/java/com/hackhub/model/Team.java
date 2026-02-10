package com.hackhub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String projectName;

    @Column(length = 1000)
    private String projectDescription;

    @Column
    private String repositoryUrl;

    @Column
    private Double score; // Valutazione del giudice (0-10)

    @Column(length = 2000)
    private String judgeFeedback;

    // ========== RELAZIONI ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    @JsonIgnore
    private Hackathon hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnore
    private User creator;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JsonIgnore
    private List<User> members = new ArrayList<>();

    // ========== COSTRUTTORI ==========

    public Team() {}

    public Team(String name, Hackathon hackathon, User creator) {
        this.name = name;
        this.hackathon = hackathon;
        this.creator = creator;
        this.addMember(creator); // Aggiunge automaticamente il creatore come membro
    }

    // ========== METODI DI UTILITÀ ==========

    /**
     * Controlla se il team ha raggiunto il numero massimo di membri
     */
    public boolean isFull() {
        if (hackathon == null || members == null) {
            return false;
        }
        return members.size() >= hackathon.getMaxTeamSize();
    }

    /**
     * Aggiunge un membro al team
     */
    public boolean addMember(User user) {
        if (user == null) {
            throw new IllegalArgumentException("L'utente non può essere null");
        }

        if (isFull()) {
            throw new IllegalStateException("Il team ha raggiunto il numero massimo di membri (" +
                    hackathon.getMaxTeamSize() + ")");
        }

        // Controlla se l'utente è già nel team
        if (members.contains(user)) {
            return false; // L'utente è già nel team
        }

        // Controlla se l'utente è già in un altro team
        if (user.getTeam() != null && !user.getTeam().equals(this)) {
            throw new IllegalStateException("L'utente è già membro di un altro team: " +
                    user.getTeam().getName());
        }

        // Aggiungi l'utente al team
        members.add(user);
        user.setTeam(this); // Imposta la relazione bidirezionale

        return true;
    }

    /**
     * Rimuove un membro dal team
     */
    public boolean removeMember(User user) {
        if (user == null) {
            return false;
        }

        if (!members.contains(user)) {
            return false; // L'utente non è nel team
        }

        // Non permettere di rimuovere il creatore del team
        if (user.equals(creator)) {
            throw new IllegalStateException("Il creatore del team non può essere rimosso");
        }

        // Rimuovi l'utente
        members.remove(user);
        user.setTeam(null); // Rimuovi la relazione

        return true;
    }

    /**
     * Controlla se un utente è membro del team
     */
    public boolean hasMember(User user) {
        return members != null && members.contains(user);
    }

    /**
     * Controlla se un utente è il creatore del team
     */
    public boolean isCreator(User user) {
        return creator != null && creator.equals(user);
    }

    /**
     * Ottiene il numero di membri attuali
     */
    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }

    /**
     * Ottiene la lista dei nomi dei membri
     */
    public List<String> getMemberNames() {
        List<String> names = new ArrayList<>();
        if (members != null) {
            for (User member : members) {
                names.add(member.getUsername());
            }
        }
        return names;
    }

    /**
     * Sottomette il progetto del team
     */
    public void submitProject(String projectName, String projectDescription, String repositoryUrl) {
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.repositoryUrl = repositoryUrl;
    }

    /**
     * Valuta il team (metodo chiamato dal giudice)
     */
    public void evaluate(Double score, String feedback) {
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("Il punteggio deve essere tra 0 e 10");
        }
        this.score = score;
        this.judgeFeedback = feedback;
    }

    /**
     * Resetta la valutazione
     */
    public void resetEvaluation() {
        this.score = null;
        this.judgeFeedback = null;
    }

    /**
     * Controlla se il team ha già inviato un progetto
     */
    public boolean hasSubmittedProject() {
        return projectName != null && !projectName.trim().isEmpty();
    }

    /**
     * Controlla se il team è stato valutato
     */
    public boolean isEvaluated() {
        return score != null;
    }

    // ========== GETTER E SETTER ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getJudgeFeedback() { return judgeFeedback; }
    public void setJudgeFeedback(String judgeFeedback) { this.judgeFeedback = judgeFeedback; }

    public Hackathon getHackathon() { return hackathon; }
    public void setHackathon(Hackathon hackathon) { this.hackathon = hackathon; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) {
        this.members = members != null ? members : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", hackathon=" + (hackathon != null ? hackathon.getName() : "null") +
                ", creator=" + (creator != null ? creator.getUsername() : "null") +
                ", members=" + getMemberCount() +
                ", projectSubmitted=" + hasSubmittedProject() +
                ", score=" + score +
                '}';
    }
}
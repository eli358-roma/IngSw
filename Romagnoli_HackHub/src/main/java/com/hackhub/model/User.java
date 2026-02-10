package com.hackhub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // "USER", "ORGANIZER", "JUDGE", "MENTOR"

    // ========== RELAZIONI ==========

    // Un utente può appartenere a un solo team
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @JsonIgnore
    private Team team;

    // Un organizzatore può creare hackathon
    @OneToMany(mappedBy = "organizer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Hackathon> organizedHackathons = new ArrayList<>();

    // Un giudice può essere assegnato a hackathon
    @OneToMany(mappedBy = "judge", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Hackathon> judgedHackathons = new ArrayList<>();

    // Un mentore può essere assegnato a hackathon (relazione many-to-many)
    @ManyToMany(mappedBy = "mentors", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Hackathon> mentoredHackathons = new ArrayList<>();

    // ========== COSTRUTTORI ==========

    public User() {}

    public User(String email, String username, String password, String role) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // ========== METODI DI UTILITÀ ==========

    public boolean joinTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("Il team non può essere null");
        }

        if (this.team != null) {
            // Se è già in un team, prima lo lascia
            leaveTeam();
        }

        return team.addMember(this);
    }

    /**
     * Lascia il team corrente
     */
    public boolean leaveTeam() {
        if (this.team == null) {
            return false; // Non è in un team
        }

        boolean removed = this.team.removeMember(this);
        if (removed) {
            this.team = null;
        }
        return removed;
    }

    /**
     * Controlla se l'utente è in un team
     */
    public boolean isInTeam() {
        return this.team != null;
    }

    /**
     * Controlla se l'utente è il creatore del suo team
     */
    public boolean isTeamCreator() {
        return this.team != null && this.team.isCreator(this);
    }

    /**
     * Controlla se l'utente può creare/gestire hackathon
     */
    public boolean canCreateHackathon() {
        return "ORGANIZER".equals(this.role);
    }

    /**
     * Controlla se l'utente può valutare progetti
     */
    public boolean canEvaluateProjects() {
        return "JUDGE".equals(this.role);
    }

    /**
     * Controlla se l'utente può fare da mentore
     */
    public boolean canMentor() {
        return "MENTOR".equals(this.role);
    }

    // ========== GETTER E SETTER ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", team=" + (team != null ? team.getName() : "none") +
                '}';
    }
}
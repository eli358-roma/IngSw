package com.hackhub.service;

import com.hackhub.model.Team;
import com.hackhub.model.User;
import com.hackhub.model.Hackathon;
import com.hackhub.repository.TeamRepository;
import com.hackhub.repository.UserRepository;
import com.hackhub.repository.HackathonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackathonRepository hackathonRepository;

    /**
     * Crea un nuovo team
     */
    public Team createTeam(String teamName, Long hackathonId, Long creatorId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new RuntimeException("Hackathon non trovato"));

        if (!hackathon.isRegistrationOpen()) {
            throw new RuntimeException("Registrazioni chiuse per questo hackathon");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (creator.getTeam() != null) {
            throw new RuntimeException("L'utente è già membro di un team");
        }

        Team team = new Team(teamName, hackathon, creator);
        Team savedTeam = teamRepository.save(team);

        System.out.println("✅ Team " + teamName + " creato con successo!");
        return savedTeam;
    }

    /**
     * Unisce un utente a un team
     */
    public Team joinTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // Usa il nuovo metodo joinTeam della classe User
        user.joinTeam(team);

        // Salva le modifiche
        userRepository.save(user);
        teamRepository.save(team);

        System.out.println("✅ Utente " + user.getUsername() + " aggiunto al team " + team.getName());
        return team;
    }

    /**
     * Rimuove un utente da un team
     */
    public Team leaveTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (!team.hasMember(user)) {
            throw new RuntimeException("L'utente non fa parte di questo team");
        }

        // Usa il metodo leaveTeam della classe User
        user.leaveTeam();

        // Salva le modifiche
        userRepository.save(user);
        teamRepository.save(team);

        System.out.println("✅ Utente " + user.getUsername() + " rimosso dal team " + team.getName());
        return team;
    }

    /**
     * Sottomette il progetto del team
     */
    public Team submitProject(Long teamId, String projectName, String description, String repoUrl) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        if (!team.getHackathon().isInProgress()) {
            throw new RuntimeException("L'hackathon non è in corso");
        }

        team.submitProject(projectName, description, repoUrl);

        Team savedTeam = teamRepository.save(team);
        System.out.println("✅ Progetto inviato per il team " + team.getName());

        return savedTeam;
    }

    /**
     * Valuta un team
     */
    public Team evaluateTeam(Long teamId, Double score, String feedback) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        team.evaluate(score, feedback);

        Team savedTeam = teamRepository.save(team);
        System.out.println("✅ Team " + team.getName() + " valutato con punteggio: " + score);

        return savedTeam;
    }

    /**
     * Ottiene tutti i team di un hackathon
     */
    public List<Team> getTeamsByHackathon(Long hackathonId) {
        return teamRepository.findByHackathonId(hackathonId);
    }

    /**
     * Ottiene un team per ID
     */
    public Team getTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));
    }

    /**
     * Ottiene tutti i team
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    /**
     * Elimina un team
     */
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team non trovato"));

        // Rimuovi tutti i membri dal team prima di eliminarlo
        for (User member : team.getMembers()) {
            member.leaveTeam();
            userRepository.save(member);
        }

        teamRepository.delete(team);
        System.out.println("✅ Team " + team.getName() + " eliminato");
    }
}
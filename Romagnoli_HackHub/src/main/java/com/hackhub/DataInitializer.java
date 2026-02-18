package com.hackhub;

import com.hackhub.model.Hackathon;
import com.hackhub.model.User;
import com.hackhub.repository.HackathonRepository;
import com.hackhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackathonRepository hackathonRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Inizializzazione dati di test...");

        // Crea utenti di test
        User organizer = new User("organizer@hackhub.com", "Mario Organizer", "password", "ORGANIZER");
        User judge = new User("judge@hackhub.com", "Luigi Giudice", "password", "JUDGE");
        User mentor = new User("mentor@hackhub.com", "Anna Mentor", "password", "MENTOR");
        User participant1 = new User("mario@example.com", "Mario Rossi", "password", "USER");
        User participant2 = new User("luigi@example.com", "Luigi Verdi", "password", "USER");

        userRepository.save(organizer);
        userRepository.save(judge);
        userRepository.save(mentor);
        userRepository.save(participant1);
        userRepository.save(participant2);

        // Crea hackathon di test
        Hackathon hackathon1 = new Hackathon(
                "AI Innovation Challenge",
                "Sviluppa soluzioni AI innovative",
                "Regolamento: 1. Team di max 4 persone, 2. Codice open source, 3. Presentazione finale",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(5),
                4,
                organizer, 5000.0 //premio di 5000€
        );

        hackathon1.setJudge(judge);
        hackathonRepository.save(hackathon1);

        Hackathon hackathon2 = new Hackathon(
                "Green Tech Hackathon",
                "Tecnologie per la sostenibilità",
                "Regolamento: 1. Focus su sostenibilità, 2. Team di max 3 persone, 3. Demo live",
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(12),
                LocalDateTime.now().plusDays(14),
                3,
                organizer,
                3000.0 //premio di 3000€
        );

        hackathonRepository.save(hackathon2);

        System.out.println("Dati di test creati con successo!");
        System.out.println("Utenti creati: " + userRepository.count());
        System.out.println("Hackathon creati: " + hackathonRepository.count());
        System.out.println("Hackathon con premi: AI Challenge (5000€), Green Tech (3000€)");
    }
}

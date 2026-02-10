package com.hackhub.repository;

import com.hackhub.model.Hackathon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HackathonRepository extends JpaRepository<Hackathon, Long> {
    List<Hackathon> findByStatus(String status);
    List<Hackathon> findByOrganizerId(Long organizerId); // Aggiungi questo metodo
}
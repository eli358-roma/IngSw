package com.hackhub.repository;

import com.hackhub.model.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
    List<SupportRequest> findByTeamId(Long teamId);
    List<SupportRequest> findByMentorId(Long mentorId);
    List<SupportRequest> findByStatus(String status);
    List<SupportRequest> findByTeamHackathonId(Long hackathonId);
}
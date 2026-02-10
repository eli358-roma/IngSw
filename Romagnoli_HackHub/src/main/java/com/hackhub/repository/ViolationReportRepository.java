package com.hackhub.repository;

import com.hackhub.model.ViolationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ViolationReportRepository extends JpaRepository<ViolationReport, Long> {
    List<ViolationReport> findByReporterId(Long reporterId);
    List<ViolationReport> findByReportedTeamId(Long teamId);
    List<ViolationReport> findByAssignedOrganizerId(Long organizerId);
    List<ViolationReport> findByStatus(String status);
    List<ViolationReport> findByHackathonId(Long hackathonId);
}
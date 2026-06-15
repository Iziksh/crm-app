package com.crm.repository;

import com.crm.domain.entity.Lead;
import com.crm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    List<Lead> findByStatus(LeadStatus status);
    Page<Lead> findByStatus(LeadStatus status, Pageable pageable);
    List<Lead> findByAssignedTo_Id(Long userId);
    List<Lead> findByAccount_Id(Long accountId);
    long countByStatus(LeadStatus status);
    List<Lead> findByStatusNot(LeadStatus status);

    @Query("SELECT l.status, COUNT(l) FROM Lead l GROUP BY l.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT l.status, COUNT(l) FROM Lead l WHERE l.workspace.id IN :ids GROUP BY l.status")
    List<Object[]> countGroupByStatusAndWorkspaceIds(@Param("ids") Collection<Long> ids);

    // L-02: Unassigned leads created before cutoff, not in terminal status
    @Query("SELECT l FROM Lead l WHERE l.assignedTo IS NULL AND l.status NOT IN :excluded AND l.createdAt <= :cutoff")
    List<Lead> findUnassigned(@Param("excluded") List<LeadStatus> excluded, @Param("cutoff") LocalDateTime cutoff);

    // L-03: Stagnant leads — status stuck in early stage for N days
    @Query("SELECT l FROM Lead l WHERE l.status IN :statuses AND l.updatedAt <= :cutoff AND l.assignedTo IS NOT NULL")
    List<Lead> findStagnant(@Param("statuses") List<LeadStatus> statuses, @Param("cutoff") LocalDateTime cutoff);

    // L-08: Close date already passed, not terminal
    @Query("SELECT l FROM Lead l WHERE l.closeDate < :today AND l.status NOT IN :excluded")
    List<Lead> findOverdueCloseDate(@Param("today") LocalDate today, @Param("excluded") List<LeadStatus> excluded);
}

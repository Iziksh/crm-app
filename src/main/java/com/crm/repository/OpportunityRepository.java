package com.crm.repository;

import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.OpportunityStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    List<Opportunity> findByStage(OpportunityStage stage);
    Page<Opportunity> findByStage(OpportunityStage stage, Pageable pageable);
    List<Opportunity> findByAssignedTo_Id(Long userId);
    List<Opportunity> findByAccount_Id(Long accountId);
    List<Opportunity> findByStageNot(OpportunityStage stage);
    long countByStage(OpportunityStage stage);

    @Query("SELECT o.stage, COUNT(o) FROM Opportunity o GROUP BY o.stage")
    List<Object[]> countGroupByStage();

    @Query("SELECT o.stage, COUNT(o) FROM Opportunity o WHERE o.workspace.id IN :ids GROUP BY o.stage")
    List<Object[]> countGroupByStageAndWorkspaceIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT SUM(o.amount) FROM Opportunity o WHERE o.stage != 'LOST'")
    BigDecimal sumPipelineAmount();

    @Query("SELECT SUM(o.amount) FROM Opportunity o WHERE o.stage != 'LOST' AND o.workspace.id IN :ids")
    BigDecimal sumPipelineAmountByWorkspaceIds(@Param("ids") Collection<Long> ids);

    // O-05: Stagnant deals not updated in N days
    @Query("SELECT o FROM Opportunity o WHERE o.stage NOT IN :excluded AND o.updatedAt <= :cutoff AND o.assignedTo IS NOT NULL")
    List<Opportunity> findStagnant(@Param("excluded") List<OpportunityStage> excluded, @Param("cutoff") LocalDateTime cutoff);

    // O-07: Close date approaching within N days
    @Query("SELECT o FROM Opportunity o WHERE o.closeDate BETWEEN :from AND :to AND o.stage NOT IN :excluded")
    List<Opportunity> findApproachingCloseDate(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("excluded") List<OpportunityStage> excluded);

    // O-08: Close date passed, not terminal
    @Query("SELECT o FROM Opportunity o WHERE o.closeDate < :today AND o.stage NOT IN :excluded")
    List<Opportunity> findOverdueCloseDate(@Param("today") LocalDate today, @Param("excluded") List<OpportunityStage> excluded);
}

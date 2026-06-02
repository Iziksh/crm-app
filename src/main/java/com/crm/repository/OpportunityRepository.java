package com.crm.repository;

import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.OpportunityStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    List<Opportunity> findByStage(OpportunityStage stage);
    Page<Opportunity> findByStage(OpportunityStage stage, Pageable pageable);
    List<Opportunity> findByAssignedTo_Id(Long userId);
    List<Opportunity> findByAccount_Id(Long accountId);
    List<Opportunity> findByStageNot(OpportunityStage stage);
    long countByStage(OpportunityStage stage);

    @Query("SELECT SUM(o.amount) FROM Opportunity o WHERE o.stage != 'LOST'")
    BigDecimal sumPipelineAmount();
}

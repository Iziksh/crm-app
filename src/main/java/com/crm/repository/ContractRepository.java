package com.crm.repository;

import com.crm.domain.entity.Contract;
import com.crm.domain.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {
    List<Contract> findByStatus(ContractStatus status);
    Page<Contract> findByStatus(ContractStatus status, Pageable pageable);
    long countByStatus(ContractStatus status);
    List<Contract> findBySalesOrder_Id(Long salesOrderId);
    List<Contract> findByAccount_Id(Long accountId);
    List<Contract> findByEndDateBefore(LocalDate date);
    long countByEndDateBefore(LocalDate date);

    @Query("SELECT c.status, COUNT(c) FROM Contract c GROUP BY c.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT c.status, COUNT(c) FROM Contract c WHERE c.workspace.id IN :ids GROUP BY c.status")
    List<Object[]> countGroupByStatusAndWorkspaceIds(@Param("ids") Collection<Long> ids);

    @Query("SELECT COUNT(c) FROM Contract c WHERE c.endDate < :date AND c.workspace.id IN :ids")
    long countByEndDateBeforeAndWorkspaceIds(@Param("date") LocalDate date, @Param("ids") Collection<Long> ids);

    // A-02: Active contracts expiring within warning window
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.endDate BETWEEN :from AND :to")
    List<Contract> findExpiringBetween(@Param("status") ContractStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

    // A-03: Contracts past end date still ACTIVE
    @Query("SELECT c FROM Contract c WHERE c.status = :status AND c.endDate < :today")
    List<Contract> findExpiredActive(@Param("status") ContractStatus status, @Param("today") LocalDate today);
}

package com.crm.repository;

import com.crm.domain.entity.Contract;
import com.crm.domain.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {
    List<Contract> findByStatus(ContractStatus status);
    Page<Contract> findByStatus(ContractStatus status, Pageable pageable);
    long countByStatus(ContractStatus status);
    List<Contract> findBySalesOrder_Id(Long salesOrderId);
    List<Contract> findByAccount_Id(Long accountId);
    List<Contract> findByEndDateBefore(LocalDate date);
}

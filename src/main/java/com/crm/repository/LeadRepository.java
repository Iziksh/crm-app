package com.crm.repository;

import com.crm.domain.entity.Lead;
import com.crm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    List<Lead> findByStatus(LeadStatus status);
    Page<Lead> findByStatus(LeadStatus status, Pageable pageable);
    List<Lead> findByAssignedTo_Id(Long userId);
    List<Lead> findByAccount_Id(Long accountId);
    long countByStatus(LeadStatus status);
    List<Lead> findByStatusNot(LeadStatus status);
}

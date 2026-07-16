package com.crm.billing.repository;

import com.crm.billing.entity.DocumentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentPaymentRepository extends JpaRepository<DocumentPayment, Long> {
    List<DocumentPayment> findByTaxDocument_Id(Long taxDocumentId);
}

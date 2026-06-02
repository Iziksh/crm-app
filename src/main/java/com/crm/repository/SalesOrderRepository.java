package com.crm.repository;

import com.crm.domain.entity.SalesOrder;
import com.crm.domain.enums.SalesOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {
    List<SalesOrder> findByStatus(SalesOrderStatus status);
    Page<SalesOrder> findByStatus(SalesOrderStatus status, Pageable pageable);
    long countByStatus(SalesOrderStatus status);
    List<SalesOrder> findByQuote_Id(Long quoteId);
    List<SalesOrder> findByAccount_Id(Long accountId);
}

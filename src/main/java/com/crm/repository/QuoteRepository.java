package com.crm.repository;

import com.crm.domain.entity.Quote;
import com.crm.domain.enums.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long>, JpaSpecificationExecutor<Quote> {
    List<Quote> findByStatus(QuoteStatus status);
    Page<Quote> findByStatus(QuoteStatus status, Pageable pageable);
    List<Quote> findByOpportunity_Id(Long opportunityId);
    List<Quote> findByAccount_Id(Long accountId);
    Optional<Quote> findByQuoteNumber(String quoteNumber);
    long countByStatus(QuoteStatus status);
    List<Quote> findByStatusNot(QuoteStatus status);
}

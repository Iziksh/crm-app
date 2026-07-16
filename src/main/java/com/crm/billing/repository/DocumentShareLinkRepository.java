package com.crm.billing.repository;

import com.crm.billing.entity.DocumentShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DocumentShareLinkRepository extends JpaRepository<DocumentShareLink, Long> {
    Optional<DocumentShareLink> findByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}

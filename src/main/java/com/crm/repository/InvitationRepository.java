package com.crm.repository;

import com.crm.domain.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByTokenHash(String tokenHash);
    void deleteByExpiresAtBeforeAndAcceptedAtIsNull(LocalDateTime cutoff);
}
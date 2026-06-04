package com.crm.repository;

import com.crm.domain.entity.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    Optional<TrustedDevice> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("DELETE FROM TrustedDevice t WHERE t.userEmail = :email")
    void deleteByUserEmail(String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM TrustedDevice t WHERE t.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}

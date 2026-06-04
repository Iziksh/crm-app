package com.crm.repository;

import com.crm.domain.entity.Alert;
import com.crm.domain.enums.AlertState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUser_IdAndAlertStateInOrderByCreatedAtDesc(Long userId, List<AlertState> states);
    long countByUser_IdAndAlertState(Long userId, AlertState state);
    Page<Alert> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    // Dedup check: find existing non-expired alert for same user + entity
    Optional<Alert> findFirstByUser_IdAndEntityTypeAndEntityIdAndResendCutoffAtAfter(
            Long userId, String entityType, Long entityId, LocalDateTime now);
    // For expiry job
    List<Alert> findByAlertStateAndCreatedAtBefore(AlertState state, LocalDateTime cutoff);
    List<Alert> findByAlertStateAndCreatedAtBeforeAndEntityIdNotNull(AlertState state, LocalDateTime cutoff);

    // Structured dedup key
    boolean existsByDedupKey(String dedupKey);
}

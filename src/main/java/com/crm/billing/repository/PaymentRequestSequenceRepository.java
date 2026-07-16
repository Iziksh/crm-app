package com.crm.billing.repository;

import com.crm.billing.entity.PaymentRequestSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRequestSequenceRepository extends JpaRepository<PaymentRequestSequence, Long> {

    /** Row-locked fetch (SELECT ... FOR UPDATE) so concurrent issuers of the same
     * (workspace, year) series serialize instead of racing on lastNumber. Must be called
     * inside an existing transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PaymentRequestSequence s WHERE s.workspaceId = :workspaceId AND s.year = :year")
    Optional<PaymentRequestSequence> findForUpdate(@Param("workspaceId") Long workspaceId, @Param("year") Integer year);

    boolean existsByWorkspaceIdAndYear(Long workspaceId, Integer year);
}

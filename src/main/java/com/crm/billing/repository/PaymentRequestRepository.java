package com.crm.billing.repository;

import com.crm.billing.entity.PaymentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    Page<PaymentRequest> findByWorkspace_IdIn(Collection<Long> workspaceIds, Pageable pageable);

    /** JOIN FETCH so workspace/account are initialized before the returned entity can outlive the
     * fetching transaction — needed by {@code ConversionService}, which orchestrates several
     * separately-transactional calls rather than one long-lived transaction/session. */
    @Query("SELECT p FROM PaymentRequest p JOIN FETCH p.workspace JOIN FETCH p.account WHERE p.id = :id AND p.workspace.id IN :ids")
    Optional<PaymentRequest> findByIdAndWorkspaceIds(@Param("id") Long id, @Param("ids") Collection<Long> ids);

    @Query("SELECT p FROM PaymentRequest p JOIN FETCH p.workspace JOIN FETCH p.account WHERE p.id = :id")
    Optional<PaymentRequest> findByIdFetched(@Param("id") Long id);
}

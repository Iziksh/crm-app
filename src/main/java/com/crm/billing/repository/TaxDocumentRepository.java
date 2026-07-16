package com.crm.billing.repository;

import com.crm.billing.entity.TaxDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface TaxDocumentRepository extends JpaRepository<TaxDocument, Long> {

    Page<TaxDocument> findByWorkspace_IdIn(Collection<Long> workspaceIds, Pageable pageable);

    /** JOIN FETCH so workspace/account are initialized before the returned entity can outlive the
     * fetching transaction — needed by {@code ConversionService}, which orchestrates several
     * separately-transactional calls rather than one long-lived transaction/session. */
    @Query("SELECT d FROM TaxDocument d JOIN FETCH d.workspace JOIN FETCH d.account WHERE d.id = :id AND d.workspace.id IN :ids")
    Optional<TaxDocument> findByIdAndWorkspaceIds(@Param("id") Long id, @Param("ids") Collection<Long> ids);

    @Query("SELECT d FROM TaxDocument d JOIN FETCH d.workspace JOIN FETCH d.account WHERE d.id = :id")
    Optional<TaxDocument> findByIdFetched(@Param("id") Long id);

    /** Used by ConversionService to make conversion idempotent: a second convert call on the same
     * payment request must return the already-created document instead of issuing a duplicate. */
    @Query("SELECT d FROM TaxDocument d JOIN FETCH d.workspace JOIN FETCH d.account WHERE d.sourcePaymentRequestId = :sourcePaymentRequestId")
    Optional<TaxDocument> findBySourcePaymentRequestId(@Param("sourcePaymentRequestId") Long sourcePaymentRequestId);
}

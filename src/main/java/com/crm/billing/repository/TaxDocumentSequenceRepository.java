package com.crm.billing.repository;

import com.crm.billing.entity.TaxDocumentSequence;
import com.crm.billing.enums.DocumentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxDocumentSequenceRepository extends JpaRepository<TaxDocumentSequence, Long> {

    /** Row-locked fetch (SELECT ... FOR UPDATE) so concurrent issuers of the same
     * (workspace, documentType, year) series serialize instead of racing on lastNumber.
     * Must be called inside an existing transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TaxDocumentSequence s WHERE s.workspaceId = :workspaceId AND s.documentType = :documentType AND s.year = :year")
    Optional<TaxDocumentSequence> findForUpdate(@Param("workspaceId") Long workspaceId,
                                                 @Param("documentType") DocumentType documentType,
                                                 @Param("year") Integer year);

    boolean existsByWorkspaceIdAndDocumentTypeAndYear(Long workspaceId, DocumentType documentType, Integer year);
}

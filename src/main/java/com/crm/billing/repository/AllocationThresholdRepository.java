package com.crm.billing.repository;

import com.crm.billing.entity.AllocationThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AllocationThresholdRepository extends JpaRepository<AllocationThreshold, Long> {

    /** The threshold in effect for a given document date: the latest row whose effectiveDate
     * is on or before that date. */
    Optional<AllocationThreshold> findTopByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(LocalDate documentDate);
}

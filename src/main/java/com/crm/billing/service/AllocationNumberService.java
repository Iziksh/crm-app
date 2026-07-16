package com.crm.billing.service;

import com.crm.billing.entity.AllocationThreshold;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.AllocationStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.VatType;
import com.crm.billing.exception.AllocationRequiredButFailedException;
import com.crm.billing.exception.MissingCustomerTaxIdException;
import com.crm.billing.repository.AllocationThresholdRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Decides whether a tax document needs a מספר הקצאה and, if so, requests it.
 *
 * ASSUMPTION / resolved ambiguity: the prompt lists "foreign customers (no Israeli tax id)" as a
 * skip condition but separately requires a blocking {@code MissingCustomerTaxIdException} for
 * "missing tax id". Since {@code Account.taxId} absence is the only signal this codebase has for
 * "no Israeli tax id" (no separate isForeign flag exists), both can't be true unconditionally.
 * Resolution: VAT type and threshold decide whether allocation is needed at all (regardless of tax
 * id) — a customer's tax id only matters once allocation is already required, at which point a
 * missing tax id blocks issuance rather than silently skipping, since an allocation number legally
 * cannot be requested without one.
 */
@Service
public class AllocationNumberService {

    private final AllocationThresholdRepository thresholdRepository;
    private final IsraelTaxAuthorityService taxAuthorityService;

    public AllocationNumberService(AllocationThresholdRepository thresholdRepository,
                                    IsraelTaxAuthorityService taxAuthorityService) {
        this.thresholdRepository = thresholdRepository;
        this.taxAuthorityService = taxAuthorityService;
    }

    /** Mutates the document's allocationStatus/allocationNumber in place. Throws
     * {@link MissingCustomerTaxIdException} or {@link AllocationRequiredButFailedException} instead
     * of returning normally when allocation is required but cannot be completed — callers must not
     * burn a document number in either case. */
    public void apply(TaxDocument document) {
        if (document.getAllocationStatus() == AllocationStatus.ISSUED && document.getAllocationNumber() != null) {
            return; // already have one — retrying issue() must not request a second allocation number
        }
        if (!isAllocationEligibleType(document.getDocumentType())) {
            document.setAllocationStatus(AllocationStatus.NOT_REQUIRED);
            return;
        }
        if (document.getVatType() != VatType.STANDARD) {
            document.setAllocationStatus(AllocationStatus.NOT_REQUIRED);
            return;
        }
        BigDecimal threshold = currentThreshold(document.getDocumentDate());
        if (document.getNetTotal() == null || document.getNetTotal().compareTo(threshold) <= 0) {
            document.setAllocationStatus(AllocationStatus.NOT_REQUIRED);
            return;
        }

        String customerTaxId = document.getAccount() != null ? document.getAccount().getTaxId() : null;
        if (customerTaxId == null || customerTaxId.isBlank()) {
            document.setAllocationStatus(AllocationStatus.FAILED);
            throw new MissingCustomerTaxIdException(
                    "Allocation number required (net " + document.getNetTotal() + " exceeds threshold " + threshold
                            + ") but customer has no tax id");
        }

        document.setAllocationStatus(AllocationStatus.PENDING);
        AllocationRequest request = new AllocationRequest(
                document.getWorkspace().getId(), document.getId(), document.getDocumentType().name(),
                document.getNetTotal(), document.getCurrency(), customerTaxId, document.getDocumentDate());
        try {
            AllocationResult result = taxAuthorityService.requestAllocationNumber(request);
            if (!result.success()) {
                document.setAllocationStatus(AllocationStatus.FAILED);
                throw new AllocationRequiredButFailedException(
                        "Allocation number request failed: " + result.errorMessage(), null);
            }
            document.setAllocationNumber(result.allocationNumber());
            document.setAllocationStatus(AllocationStatus.ISSUED);
        } catch (AllocationRequiredButFailedException e) {
            throw e;
        } catch (RuntimeException e) {
            document.setAllocationStatus(AllocationStatus.FAILED);
            throw new AllocationRequiredButFailedException("Allocation number request failed", e);
        }
    }

    private boolean isAllocationEligibleType(DocumentType type) {
        return type == DocumentType.TAX_INVOICE || type == DocumentType.TAX_INVOICE_RECEIPT;
    }

    private BigDecimal currentThreshold(LocalDate documentDate) {
        LocalDate effectiveAsOf = documentDate != null ? documentDate : LocalDate.now();
        // Fail-safe: if no threshold row is configured, treat the threshold as 0 (always exceeded)
        // rather than silently skipping allocation — over-requesting is safer than under-requesting.
        return thresholdRepository.findTopByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(effectiveAsOf)
                .map(AllocationThreshold::getThresholdAmount)
                .orElse(BigDecimal.ZERO);
    }
}

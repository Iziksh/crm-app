package com.crm.billing.controller;

import com.crm.billing.dto.DocumentPaymentRequest;
import com.crm.billing.dto.DocumentPaymentResponse;
import com.crm.billing.dto.IssueResultResponse;
import com.crm.billing.dto.TaxDocumentCreateRequest;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.service.TaxDocumentDraftService;
import com.crm.billing.service.TaxDocumentIssueService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing/tax-documents")
public class TaxDocumentController {

    private final TaxDocumentDraftService draftService;
    private final TaxDocumentIssueService issueService;

    public TaxDocumentController(TaxDocumentDraftService draftService, TaxDocumentIssueService issueService) {
        this.draftService = draftService;
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<TaxDocumentResponse> create(@Valid @RequestBody TaxDocumentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TaxDocumentResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(draftService.findAll(pageable).getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxDocumentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(draftService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxDocumentResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody TaxDocumentCreateRequest request) {
        return ResponseEntity.ok(draftService.update(id, request));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<IssueResultResponse> issue(@PathVariable Long id) {
        return ResponseEntity.ok(issueService.issue(id));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<DocumentPaymentResponse> addPayment(@PathVariable Long id,
                                                               @Valid @RequestBody DocumentPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(draftService.addPayment(id, request));
    }
}

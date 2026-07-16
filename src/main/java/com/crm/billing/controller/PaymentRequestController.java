package com.crm.billing.controller;

import com.crm.billing.dto.ConvertRequest;
import com.crm.billing.dto.PaymentRequestCreateRequest;
import com.crm.billing.dto.PaymentRequestResponse;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.service.ConversionService;
import com.crm.billing.service.PaymentRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing/payment-requests")
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;
    private final ConversionService conversionService;

    public PaymentRequestController(PaymentRequestService paymentRequestService, ConversionService conversionService) {
        this.paymentRequestService = paymentRequestService;
        this.conversionService = conversionService;
    }

    @PostMapping
    public ResponseEntity<PaymentRequestResponse> create(@Valid @RequestBody PaymentRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentRequestService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentRequestResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(paymentRequestService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentRequestService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentRequestResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody PaymentRequestCreateRequest request) {
        return ResponseEntity.ok(paymentRequestService.update(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        paymentRequestService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<TaxDocumentResponse> convert(@PathVariable Long id, @Valid @RequestBody ConvertRequest request) {
        return ResponseEntity.ok(conversionService.convert(id, request.targetDocumentType()));
    }
}

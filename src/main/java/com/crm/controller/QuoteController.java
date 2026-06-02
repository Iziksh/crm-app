package com.crm.controller;

import com.crm.domain.enums.QuoteStatus;
import com.crm.dto.request.QuoteLineItemRequest;
import com.crm.dto.request.QuoteRequest;
import com.crm.dto.response.QuoteResponse;
import com.crm.dto.response.SalesOrderResponse;
import com.crm.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @PostMapping
    public ResponseEntity<QuoteResponse> create(
            @Valid @RequestBody QuoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quoteService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) QuoteStatus status,
            Pageable pageable) {
        if (status != null) return ResponseEntity.ok(quoteService.findByStatus(status));
        Page<QuoteResponse> page = quoteService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuoteResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(quoteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/line-items")
    public ResponseEntity<QuoteResponse> addLineItem(@PathVariable Long id,
                                                     @Valid @RequestBody QuoteLineItemRequest request) {
        return ResponseEntity.ok(quoteService.addLineItem(id, request));
    }

    @DeleteMapping("/{id}/line-items/{lineId}")
    public ResponseEntity<QuoteResponse> removeLineItem(@PathVariable Long id,
                                                        @PathVariable Long lineId) {
        return ResponseEntity.ok(quoteService.removeLineItem(id, lineId));
    }

    @PostMapping("/{id}/convert-to-order")
    public ResponseEntity<SalesOrderResponse> convertToOrder(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.convertToOrder(id));
    }
}

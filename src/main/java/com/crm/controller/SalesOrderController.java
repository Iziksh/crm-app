package com.crm.controller;

import com.crm.domain.enums.SalesOrderStatus;
import com.crm.dto.request.SalesOrderLineItemRequest;
import com.crm.dto.request.SalesOrderRequest;
import com.crm.dto.response.ContractResponse;
import com.crm.dto.response.SalesOrderResponse;
import com.crm.service.SalesOrderService;
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
@RequestMapping("/api/v1/sales-orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping
    public ResponseEntity<SalesOrderResponse> create(
            @Valid @RequestBody SalesOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(salesOrderService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) SalesOrderStatus status,
            Pageable pageable) {
        if (status != null) return ResponseEntity.ok(salesOrderService.findByStatus(status));
        Page<SalesOrderResponse> page = salesOrderService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody SalesOrderRequest request) {
        return ResponseEntity.ok(salesOrderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/line-items")
    public ResponseEntity<SalesOrderResponse> addLineItem(@PathVariable Long id,
                                                          @Valid @RequestBody SalesOrderLineItemRequest request) {
        return ResponseEntity.ok(salesOrderService.addLineItem(id, request));
    }

    @DeleteMapping("/{id}/line-items/{lineId}")
    public ResponseEntity<SalesOrderResponse> removeLineItem(@PathVariable Long id,
                                                             @PathVariable Long lineId) {
        return ResponseEntity.ok(salesOrderService.removeLineItem(id, lineId));
    }

    @PostMapping("/{id}/convert-to-contract")
    public ResponseEntity<ContractResponse> convertToContract(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.convertToContract(id));
    }
}

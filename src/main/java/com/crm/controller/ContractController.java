package com.crm.controller;

import com.crm.domain.enums.ContractStatus;
import com.crm.dto.request.ContractRequest;
import com.crm.dto.response.ContractResponse;
import com.crm.service.ContractService;
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
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    public ResponseEntity<ContractResponse> create(
            @Valid @RequestBody ContractRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contractService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) ContractStatus status,
            Pageable pageable) {
        if (status != null) return ResponseEntity.ok(contractService.findByStatus(status));
        Page<ContractResponse> page = contractService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ContractRequest request) {
        return ResponseEntity.ok(contractService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.crm.controller;

import com.crm.dto.request.AddonRequest;
import com.crm.dto.response.AddonResponse;
import com.crm.service.AddonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/addons")
public class AddonController {

    private final AddonService addonService;

    public AddonController(AddonService addonService) {
        this.addonService = addonService;
    }

    @GetMapping
    public ResponseEntity<List<AddonResponse>> list(@PathVariable Long accountId) {
        return ResponseEntity.ok(addonService.findByAccount(accountId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddonResponse> create(@PathVariable Long accountId, @Valid @RequestBody AddonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addonService.create(accountId, request));
    }

    @PutMapping("/{addonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AddonResponse> update(@PathVariable Long accountId, @PathVariable Long addonId,
                                                 @Valid @RequestBody AddonRequest request) {
        return ResponseEntity.ok(addonService.update(addonId, request));
    }

    @DeleteMapping("/{addonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long accountId, @PathVariable Long addonId) {
        addonService.delete(addonId);
        return ResponseEntity.noContent().build();
    }
}

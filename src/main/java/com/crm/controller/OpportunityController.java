package com.crm.controller;

import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.request.OpportunityRequest;
import com.crm.dto.response.OpportunityResponse;
import com.crm.dto.response.QuoteResponse;
import com.crm.service.OpportunityService;
import com.crm.util.CsvExporter;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @PostMapping
    public ResponseEntity<OpportunityResponse> create(
            @Valid @RequestBody OpportunityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(opportunityService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) OpportunityStage stage,
            Pageable pageable) {
        if (stage != null) return ResponseEntity.ok(opportunityService.findByStage(stage));
        Page<OpportunityResponse> page = opportunityService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpportunityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OpportunityResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody OpportunityRequest request) {
        return ResponseEntity.ok(opportunityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        opportunityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/quotes")
    public ResponseEntity<List<QuoteResponse>> getQuotes(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.findQuotes(id));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(
            @RequestParam(required = false) OpportunityStage stage,
            @RequestParam(required = false) String search) {
        String[] headers = {"id","name","stage","amount","currency","probability","close_date","account","assigned_to"};
        List<String[]> rows = opportunityService.findAllForExport(stage, search).stream().map(o -> new String[]{
                CsvExporter.str(o.id()), o.name(), CsvExporter.str(o.stage()), CsvExporter.str(o.amount()),
                CsvExporter.str(o.currency()), CsvExporter.str(o.probability()), CsvExporter.str(o.closeDate()),
                CsvExporter.str(o.accountName()), CsvExporter.str(o.assignedToName())
        }).toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"opportunities.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(CsvExporter.build(headers, rows)));
    }
}

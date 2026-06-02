package com.crm.controller;

import com.crm.domain.enums.LeadStatus;
import com.crm.dto.request.LeadRequest;
import com.crm.dto.response.LeadResponse;
import com.crm.dto.response.OpportunityResponse;
import com.crm.service.LeadService;
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
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) LeadStatus status,
            Pageable pageable) {
        if (status != null) return ResponseEntity.ok(leadService.findByStatus(status));
        Page<LeadResponse> page = leadService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody LeadRequest request) {
        return ResponseEntity.ok(leadService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<OpportunityResponse> convert(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.convert(id));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) String search) {
        String[] headers = {"id","title","first_name","last_name","email","company","status","source","estimated_value","currency","close_date","assigned_to"};
        List<String[]> rows = leadService.findAllForExport(status, search).stream().map(l -> new String[]{
                CsvExporter.str(l.id()), l.title(), CsvExporter.str(l.firstName()), CsvExporter.str(l.lastName()),
                CsvExporter.str(l.email()), CsvExporter.str(l.company()), CsvExporter.str(l.status()),
                CsvExporter.str(l.source()), CsvExporter.str(l.estimatedValue()), CsvExporter.str(l.currency()),
                CsvExporter.str(l.closeDate()), CsvExporter.str(l.assignedToName())
        }).toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leads.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(CsvExporter.build(headers, rows)));
    }
}

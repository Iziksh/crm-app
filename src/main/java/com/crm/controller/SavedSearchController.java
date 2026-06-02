package com.crm.controller;

import com.crm.domain.enums.SavedSearchScope;
import com.crm.dto.request.SavedSearchRequest;
import com.crm.dto.response.SavedSearchResponse;
import com.crm.service.SavedSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/saved-searches")
public class SavedSearchController {

    private final SavedSearchService savedSearchService;

    public SavedSearchController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    @PostMapping
    public ResponseEntity<SavedSearchResponse> create(
            @Valid @RequestBody SavedSearchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedSearchService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<SavedSearchResponse>> list(
            @RequestParam(required = false) SavedSearchScope scope) {
        if (scope != null) return ResponseEntity.ok(savedSearchService.findByScope(scope));
        return ResponseEntity.ok(savedSearchService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavedSearchResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody SavedSearchRequest request) {
        return ResponseEntity.ok(savedSearchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        savedSearchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Long>> execute(@PathVariable Long id) {
        long count = savedSearchService.execute(id);
        return ResponseEntity.ok(Map.of("count", count));
    }
}

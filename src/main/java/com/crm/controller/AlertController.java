package com.crm.controller;

import com.crm.domain.enums.AlertState;
import com.crm.dto.response.AlertResponse;
import com.crm.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAlerts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) List<AlertState> state) {
        return ResponseEntity.ok(alertService.getForUser(userDetails.getUsername(), state));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<AlertResponse>> getAlertsPaged(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        return ResponseEntity.ok(alertService.getPagedForUser(userDetails.getUsername(), pageable));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countUnread(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(alertService.countUnread(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.findById(id));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<AlertResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }

    @PatchMapping("/{id}/accepted")
    public ResponseEntity<AlertResponse> markAsAccepted(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markAsAccepted(id));
    }

    @PatchMapping("/{id}/new")
    public ResponseEntity<AlertResponse> markAsNew(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markAsNew(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        alertService.markAllRead(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        alertService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

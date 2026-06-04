package com.crm.controller;

import com.crm.dto.request.SubscriptionRequest;
import com.crm.dto.response.SubscriptionResponse;
import com.crm.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @Valid @RequestBody SubscriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        SubscriptionResponse response = subscriptionService.create(request, userDetails.getUsername());
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(subscriptionService.findAllForUser(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(subscriptionService.update(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

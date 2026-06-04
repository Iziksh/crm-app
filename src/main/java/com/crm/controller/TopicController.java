package com.crm.controller;

import com.crm.dto.response.TopicResponse;
import com.crm.service.TopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ResponseEntity<List<TopicResponse>> list() {
        return ResponseEntity.ok(topicService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(topicService.findById(id));
    }

    @PutMapping("/{id}/flags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TopicResponse> updateFlags(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean sendAlert = Boolean.TRUE.equals(body.get("sendAlertEnabled"));
        boolean sendMail = Boolean.TRUE.equals(body.get("sendMailEnabled"));
        return ResponseEntity.ok(topicService.updateFlags(id, sendAlert, sendMail));
    }
}

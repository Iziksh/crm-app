package com.crm.controller;

import com.crm.domain.enums.TaskStatus;
import com.crm.dto.response.ScheduledTaskResponse;
import com.crm.service.ScheduledTaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/scheduled-tasks")
@PreAuthorize("hasRole('ADMIN')")
public class ScheduledTaskController {

    private final ScheduledTaskService taskService;

    public ScheduledTaskController(ScheduledTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<Page<ScheduledTaskResponse>> list(
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(status != null
                ? taskService.findByStatus(status, pageable)
                : taskService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledTaskResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of(
                "pending",   taskService.countByStatus(TaskStatus.PENDING),
                "failed",    taskService.countByStatus(TaskStatus.FAILED),
                "suspended", taskService.countByStatus(TaskStatus.SUSPENDED),
                "completedToday", taskService.countCompletedToday()
        ));
    }

    @PostMapping("/{id}/run-now")
    public ResponseEntity<ScheduledTaskResponse> runNow(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.runNow(id));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ScheduledTaskResponse> suspend(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return ResponseEntity.ok(taskService.suspend(id, reason));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ScheduledTaskResponse> resume(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.resume(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ScheduledTaskResponse> cancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "Cancelled by admin") : "Cancelled by admin";
        return ResponseEntity.ok(taskService.cancel(id, reason));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ScheduledTaskResponse> retry(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.retry(id));
    }
}

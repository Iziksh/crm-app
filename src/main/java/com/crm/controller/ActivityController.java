package com.crm.controller;

import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ActivityType;
import com.crm.dto.request.ActivityNoteRequest;
import com.crm.dto.request.ActivityRequest;
import com.crm.dto.response.ActivityNoteResponse;
import com.crm.dto.response.ActivityResponse;
import com.crm.service.ActivityService;
import com.crm.util.CsvExporter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<ActivityResponse> create(
            @Valid @RequestBody ActivityRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.create(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivityStatus status,
            Pageable pageable) {
        if (type != null) return ResponseEntity.ok(activityService.findByType(type));
        if (status != null) return ResponseEntity.ok(activityService.findByStatus(status));
        Page<ActivityResponse> page = activityService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ActivityResponse> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.resolve(id));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ActivityResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.close(id));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ActivityNoteResponse> addNote(
            @PathVariable Long id,
            @Valid @RequestBody ActivityNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(activityService.addNote(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id, @PathVariable Long noteId) {
        activityService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ActivityResponse> assign(@PathVariable Long id,
                                                   @RequestParam String username) {
        return ResponseEntity.ok(activityService.assign(id, username));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<ActivityResponse> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.reopen(id));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivityStatus status,
            @RequestParam(required = false) String search) {
        String[] headers = {"id","title","type","status","priority","due_date","assigned_to","account","contact"};
        List<String[]> rows = activityService.findAllForExport(type, status, search).stream().map(a -> new String[]{
                CsvExporter.str(a.id()), a.title(), CsvExporter.str(a.type()), CsvExporter.str(a.status()),
                CsvExporter.str(a.priority()), CsvExporter.str(a.dueDate()),
                CsvExporter.str(a.assignedToName()), CsvExporter.str(a.accountName()), CsvExporter.str(a.contactName())
        }).toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"activities.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(CsvExporter.build(headers, rows)));
    }
}

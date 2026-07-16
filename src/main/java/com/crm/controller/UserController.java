package com.crm.controller;

import com.crm.domain.entity.User;
import com.crm.dto.request.UserRequest;
import com.crm.dto.response.UserResponse;
import com.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** The caller's own direct reports — open to any authenticated user (manager status is data-driven, not role-based). */
    @GetMapping("/my-direct-reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponse>> myDirectReports(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.findDirectReports(user.getId()));
    }

    /**
     * Also open to ROLE_HR_MANAGER (unlike the rest of this controller): the attendance pickers
     * need a way to list company users, but that role should never reach create/update/delete/toggle.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR_MANAGER')")
    public ResponseEntity<List<UserResponse>> list(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable, search).getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<UserResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleEnabled(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

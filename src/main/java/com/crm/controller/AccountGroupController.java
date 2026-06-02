package com.crm.controller;

import com.crm.dto.request.AccountGroupRequest;
import com.crm.dto.response.AccountGroupResponse;
import com.crm.service.AccountGroupService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account-groups")
public class AccountGroupController {

    private final AccountGroupService accountGroupService;

    public AccountGroupController(AccountGroupService accountGroupService) {
        this.accountGroupService = accountGroupService;
    }

    @PostMapping
    public ResponseEntity<AccountGroupResponse> create(@Valid @RequestBody AccountGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountGroupService.create(request));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            List<AccountGroupResponse> results = accountGroupService.search(search);
            return ResponseEntity.ok(results);
        }
        Page<AccountGroupResponse> page = accountGroupService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountGroupResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountGroupService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountGroupResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody AccountGroupRequest request) {
        return ResponseEntity.ok(accountGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members/{accountId}")
    public ResponseEntity<AccountGroupResponse> addMember(@PathVariable Long id,
                                                          @PathVariable Long accountId) {
        return ResponseEntity.ok(accountGroupService.addMember(id, accountId));
    }

    @DeleteMapping("/{id}/members/{accountId}")
    public ResponseEntity<AccountGroupResponse> removeMember(@PathVariable Long id,
                                                             @PathVariable Long accountId) {
        return ResponseEntity.ok(accountGroupService.removeMember(id, accountId));
    }
}

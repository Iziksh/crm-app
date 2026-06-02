package com.crm.controller;

import com.crm.dto.request.AccountRequest;
import com.crm.dto.response.AccountResponse;
import com.crm.dto.response.AddressResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.ImportResultResponse;
import com.crm.service.AccountService;
import com.crm.service.AddressService;
import com.crm.service.ContactService;
import com.crm.service.ImportService;
import com.crm.util.CsvExporter;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final ContactService contactService;
    private final AddressService addressService;
    private final ImportService importService;

    public AccountController(AccountService accountService, ContactService contactService,
                             AddressService addressService, ImportService importService) {
        this.accountService = accountService;
        this.contactService = contactService;
        this.addressService = addressService;
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(request));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(accountService.search(search));
        }
        Page<AccountResponse> page = accountService.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.ok(accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/contacts")
    public ResponseEntity<List<ContactResponse>> getContacts(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.findByAccount(id));
    }

    @GetMapping("/{id}/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.findByAccount(id));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(@RequestParam(required = false) String search) {
        String[] headers = {"id","name","industry","website","phone","email","address","type","notes"};
        List<String[]> rows = accountService.findAllForExport(search).stream().map(a -> new String[]{
                CsvExporter.str(a.id()), a.name(), CsvExporter.str(a.industry()), CsvExporter.str(a.website()),
                CsvExporter.str(a.phone()), CsvExporter.str(a.email()), CsvExporter.str(a.address()),
                CsvExporter.str(a.type()), CsvExporter.str(a.notes())
        }).toList();
        return csv("accounts.csv", headers, rows);
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(importService.importAccounts(file.getInputStream()));
    }

    private static ResponseEntity<InputStreamResource> csv(String filename, String[] headers, List<String[]> rows) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(CsvExporter.build(headers, rows)));
    }
}

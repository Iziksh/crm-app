package com.crm.controller;

import com.crm.dto.request.ContactRequest;
import com.crm.dto.response.AddressResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.dto.response.ImportResultResponse;
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
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;
    private final AddressService addressService;
    private final ImportService importService;

    public ContactController(ContactService contactService, AddressService addressService,
                             ImportService importService) {
        this.contactService = contactService;
        this.addressService = addressService;
        this.importService = importService;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<ContactResponse>> list(
            @RequestParam(required = false) String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(contactService.search(search, pageable));
        }
        return ResponseEntity.ok(contactService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(contactService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.findByContact(id));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(@RequestParam(required = false) String search) {
        String[] headers = {"id","first_name","last_name","email","phone","job_title","department","status","account"};
        List<String[]> rows = contactService.findAllForExport(search).stream().map(c -> new String[]{
                CsvExporter.str(c.id()), c.firstName(), c.lastName(), c.email(),
                CsvExporter.str(c.phone()), CsvExporter.str(c.jobTitle()), CsvExporter.str(c.department()),
                CsvExporter.str(c.status()), CsvExporter.str(c.accountName())
        }).toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contacts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(CsvExporter.build(headers, rows)));
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(importService.importContacts(file.getInputStream()));
    }
}

package com.crm.controller;

import com.crm.dto.request.ContactRequest;
import com.crm.dto.response.AddressResponse;
import com.crm.dto.response.ContactResponse;
import com.crm.service.AddressService;
import com.crm.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;
    private final AddressService addressService;

    public ContactController(ContactService contactService, AddressService addressService) {
        this.contactService = contactService;
        this.addressService = addressService;
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
}

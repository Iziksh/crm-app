package com.crm.controller;

import com.crm.dto.request.AddressRequest;
import com.crm.dto.response.AddressResponse;
import com.crm.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long contactId) {
        if (accountId != null) return ResponseEntity.ok(addressService.findByAccount(accountId));
        if (contactId != null) return ResponseEntity.ok(addressService.findByContact(contactId));
        return ResponseEntity.ok(addressService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<AddressResponse> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(addressService.toggleEnabled(id));
    }
}

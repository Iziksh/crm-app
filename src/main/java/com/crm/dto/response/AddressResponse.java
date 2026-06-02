package com.crm.dto.response;

import com.crm.domain.entity.Address;
import com.crm.domain.enums.AddressType;

import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        AddressType type,
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        boolean enabled,
        Long accountId,
        String accountName,
        Long contactId,
        String contactName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getType(),
                a.getStreet(),
                a.getCity(),
                a.getState(),
                a.getPostalCode(),
                a.getCountry(),
                a.isEnabled(),
                a.getAccount() != null ? a.getAccount().getId() : null,
                a.getAccount() != null ? a.getAccount().getName() : null,
                a.getContact() != null ? a.getContact().getId() : null,
                a.getContact() != null ? (a.getContact().getFirstName() + " " + a.getContact().getLastName()) : null,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}

package com.crm.dto.request;

import com.crm.domain.enums.AddressType;

public record AddressRequest(
        AddressType type,
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        Long accountId,
        Long contactId
) {}

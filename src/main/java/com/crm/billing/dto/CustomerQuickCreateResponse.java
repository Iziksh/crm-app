package com.crm.billing.dto;

import com.crm.domain.entity.Account;

public record CustomerQuickCreateResponse(
        Long id,
        String name,
        String taxId,
        String phone,
        String email
) {
    public static CustomerQuickCreateResponse from(Account account) {
        return new CustomerQuickCreateResponse(account.getId(), account.getName(), account.getTaxId(),
                account.getPhone(), account.getEmail());
    }
}

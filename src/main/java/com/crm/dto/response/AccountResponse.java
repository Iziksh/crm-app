package com.crm.dto.response;

import com.crm.domain.entity.Account;
import com.crm.domain.enums.AccountType;

import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String name,
        String industry,
        String website,
        String phone,
        String email,
        String address,
        AccountType type,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getIndustry(),
                account.getWebsite(),
                account.getPhone(),
                account.getEmail(),
                account.getAddress(),
                account.getType(),
                account.getNotes(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}

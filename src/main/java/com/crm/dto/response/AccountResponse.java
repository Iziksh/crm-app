package com.crm.dto.response;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.Addon;
import com.crm.domain.enums.AccountType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime updatedAt,
        List<AddonSummary> addons
) {
    public record AddonSummary(Long id, String name, LocalDate expiryDate) {
        public static AddonSummary from(Addon addon) {
            return new AddonSummary(addon.getId(), addon.getName(), addon.getExpiryDate());
        }
    }

    public static AccountResponse from(Account account) {
        return from(account, List.of());
    }

    public static AccountResponse from(Account account, List<Addon> addons) {
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
                account.getUpdatedAt(),
                addons.stream().map(AddonSummary::from).toList()
        );
    }
}

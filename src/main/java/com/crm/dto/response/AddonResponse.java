package com.crm.dto.response;

import com.crm.domain.entity.Addon;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AddonResponse(
        Long id,
        Long accountId,
        String name,
        String description,
        LocalDate expiryDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AddonResponse from(Addon addon) {
        return new AddonResponse(
                addon.getId(),
                addon.getAccount().getId(),
                addon.getName(),
                addon.getDescription(),
                addon.getExpiryDate(),
                addon.getCreatedAt(),
                addon.getUpdatedAt()
        );
    }
}

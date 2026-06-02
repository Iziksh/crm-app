package com.crm.dto.response;

import com.crm.domain.entity.SavedSearch;
import com.crm.domain.enums.SavedSearchScope;

import java.time.LocalDateTime;

public record SavedSearchResponse(
        Long id,
        String name,
        SavedSearchScope scope,
        String filterJson,
        Long ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SavedSearchResponse from(SavedSearch s) {
        return new SavedSearchResponse(
                s.getId(),
                s.getName(),
                s.getScope(),
                s.getFilterJson(),
                s.getOwner() != null ? s.getOwner().getId() : null,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}

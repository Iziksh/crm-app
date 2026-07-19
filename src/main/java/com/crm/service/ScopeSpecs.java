package com.crm.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared list-filtering predicates for the two scoping axes every list endpoint honours:
 * the caller's workspace membership, and the globally-selected account.
 *
 * <p>Both are narrowing filters — an absent value adds no predicate, which is what lets an
 * admin with "All Accounts" selected keep seeing everything they are entitled to.
 */
public final class ScopeSpecs {

    private ScopeSpecs() {}

    /**
     * @param isAdmin      unscoped admin; skips the workspace restriction entirely
     * @param workspaceIds workspaces the caller belongs to. Empty matches nothing, so a user
     *                     with no workspace sees no rows rather than falling through unfiltered.
     * @param accountId    selected account, or null for "All Accounts"
     * @param searchFields string fields to OR together for a case-insensitive LIKE
     */
    public static <T> Specification<T> scoped(boolean isAdmin, List<Long> workspaceIds,
                                              Long accountId, String search, String... searchFields) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin) {
                predicates.add(workspaceIds.isEmpty()
                        ? cb.disjunction()
                        : root.get("workspace").get("id").in(workspaceIds));
            }
            if (accountId != null) {
                predicates.add(cb.equal(root.get("account").get("id"), accountId));
            }
            if (search != null && !search.isBlank() && searchFields.length > 0) {
                String q = "%" + search.toLowerCase() + "%";
                Predicate[] matches = new Predicate[searchFields.length];
                for (int i = 0; i < searchFields.length; i++) {
                    matches[i] = cb.like(cb.lower(root.get(searchFields[i])), q);
                }
                predicates.add(cb.or(matches));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Variant for entities with no workspace column of their own (e.g. Address). */
    public static <T> Specification<T> accountScoped(Long accountId, String search, String... searchFields) {
        return scoped(true, List.of(), accountId, search, searchFields);
    }
}

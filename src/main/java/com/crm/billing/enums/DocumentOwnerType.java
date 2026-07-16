package com.crm.billing.enums;

/** Which owning document a {@code DocumentLineItem} row belongs to (polymorphic owner,
 * same pattern as {@code Attachment.entityType}/{@code entityId} from Phase 16). */
public enum DocumentOwnerType {
    PAYMENT_REQUEST, TAX_DOCUMENT
}

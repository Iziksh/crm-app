package com.crm.billing.enums;

/** Tax document type. TAX_INVOICE / TAX_INVOICE_RECEIPT require an allocation number above threshold;
 * QUOTE/PROFORMA never do; RECEIPT is the only type an EXEMPT_DEALER workspace may issue. */
public enum DocumentType {
    TAX_INVOICE,
    TAX_INVOICE_RECEIPT,
    RECEIPT,
    PROFORMA,
    QUOTE,
    CREDIT_INVOICE
}

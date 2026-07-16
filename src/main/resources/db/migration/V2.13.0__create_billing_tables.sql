-- Billing & Documents addon (Phase 26): payment requests, tax documents, shared line items,
-- payments-received, gap-free numbering sequences, and the effective-dated allocation threshold.

CREATE TABLE payment_requests (
    id                     BIGSERIAL     PRIMARY KEY,
    workspace_id           BIGINT        NOT NULL REFERENCES workspaces(id),
    account_id             BIGINT        NOT NULL REFERENCES accounts(id),
    number                 VARCHAR(50),
    document_year          INT,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    currency               VARCHAR(3)    NOT NULL DEFAULT 'ILS',
    document_date          DATE          NOT NULL DEFAULT CURRENT_DATE,
    free_text              TEXT,
    total_amount           NUMERIC(15,2) NOT NULL DEFAULT 0,
    converted_document_id  BIGINT,
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payment_requests_workspace_id ON payment_requests(workspace_id);
CREATE INDEX idx_payment_requests_account_id ON payment_requests(account_id);

CREATE TABLE tax_documents (
    id                          BIGSERIAL     PRIMARY KEY,
    workspace_id                BIGINT        NOT NULL REFERENCES workspaces(id),
    account_id                  BIGINT        NOT NULL REFERENCES accounts(id),
    document_type                VARCHAR(30)  NOT NULL,
    number                      VARCHAR(50),
    document_year                INT,
    status                      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    currency                    VARCHAR(3)    NOT NULL DEFAULT 'ILS',
    document_date                DATE         NOT NULL DEFAULT CURRENT_DATE,
    free_text                   TEXT,
    vat_type                    VARCHAR(20)   NOT NULL DEFAULT 'STANDARD',
    discount_type                VARCHAR(20),
    discount_value               NUMERIC(15,2) NOT NULL DEFAULT 0,
    rounding_mode                VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    net_total                   NUMERIC(15,2) NOT NULL DEFAULT 0,
    vat_rate                    NUMERIC(5,4)  NOT NULL DEFAULT 0,
    vat_amount                  NUMERIC(15,2) NOT NULL DEFAULT 0,
    pre_round_total              NUMERIC(15,2) NOT NULL DEFAULT 0,
    rounding_delta                NUMERIC(15,2) NOT NULL DEFAULT 0,
    gross_total                 NUMERIC(15,2) NOT NULL DEFAULT 0,
    allocation_status            VARCHAR(20)  NOT NULL DEFAULT 'NOT_REQUIRED',
    allocation_number             VARCHAR(50),
    original_document_id          BIGINT,
    source_payment_request_id     BIGINT REFERENCES payment_requests(id),
    issued_at                   TIMESTAMP,
    created_at                  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tax_documents_workspace_id ON tax_documents(workspace_id);
CREATE INDEX idx_tax_documents_account_id ON tax_documents(account_id);
CREATE INDEX idx_tax_documents_original_document_id ON tax_documents(original_document_id);

CREATE TABLE document_line_items (
    id                  BIGSERIAL     PRIMARY KEY,
    owner_type          VARCHAR(20)   NOT NULL,
    owner_id            BIGINT        NOT NULL,
    product_or_service   VARCHAR(255) NOT NULL,
    description         TEXT,
    quantity            NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit_price           NUMERIC(15,2) NOT NULL DEFAULT 0,
    line_total           NUMERIC(15,2) NOT NULL DEFAULT 0,
    sort_order           INT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_document_line_items_owner ON document_line_items(owner_type, owner_id);

CREATE TABLE document_payments (
    id                BIGSERIAL     PRIMARY KEY,
    tax_document_id   BIGINT        NOT NULL REFERENCES tax_documents(id) ON DELETE CASCADE,
    payment_method    VARCHAR(20)   NOT NULL,
    amount            NUMERIC(15,2) NOT NULL,
    received_at       DATE          NOT NULL DEFAULT CURRENT_DATE
);
CREATE INDEX idx_document_payments_tax_document_id ON document_payments(tax_document_id);

CREATE TABLE payment_request_sequences (
    id            BIGSERIAL PRIMARY KEY,
    workspace_id  BIGINT    NOT NULL REFERENCES workspaces(id),
    year          INT       NOT NULL,
    last_number   BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_request_sequences UNIQUE (workspace_id, year)
);

CREATE TABLE tax_document_sequences (
    id             BIGSERIAL   PRIMARY KEY,
    workspace_id   BIGINT      NOT NULL REFERENCES workspaces(id),
    document_type  VARCHAR(30) NOT NULL,
    year           INT         NOT NULL,
    last_number    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_tax_document_sequences UNIQUE (workspace_id, document_type, year)
);

CREATE TABLE allocation_thresholds (
    id                BIGSERIAL     PRIMARY KEY,
    threshold_amount  NUMERIC(15,2) NOT NULL,
    effective_date    DATE          NOT NULL
);

-- Israeli מספר הקצאה threshold schedule: 10,000 ₪ from 2026-01-01, dropping to 5,000 ₪ from 2026-06-01.
INSERT INTO allocation_thresholds (threshold_amount, effective_date) VALUES (10000.00, '2026-01-01');
INSERT INTO allocation_thresholds (threshold_amount, effective_date) VALUES (5000.00, '2026-06-01');

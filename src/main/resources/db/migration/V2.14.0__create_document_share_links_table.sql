-- Billing & Documents addon: public, expiring, single-document download links used by WhatsApp
-- sharing (wa.me/api.whatsapp.com links can only pre-fill text, never attach a file).

CREATE TABLE document_share_links (
    id          BIGSERIAL    PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    owner_type  VARCHAR(20)  NOT NULL,
    owner_id    BIGINT       NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_document_share_links_expires_at ON document_share_links(expires_at);

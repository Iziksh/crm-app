CREATE TABLE invitations (
    id           BIGSERIAL    PRIMARY KEY,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL,
    role         VARCHAR(50)  NOT NULL,
    workspace_id BIGINT       NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    expires_at   TIMESTAMP    NOT NULL,
    accepted_at  TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invitations_token_hash ON invitations(token_hash);
CREATE INDEX idx_invitations_email      ON invitations(email);

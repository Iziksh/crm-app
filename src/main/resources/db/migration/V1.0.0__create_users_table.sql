-- Base users table (required by attendance FK migrations in V2.x).
-- In dev, Hibernate also creates/updates this table via ddl-auto=update.

CREATE TABLE IF NOT EXISTS users (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username     VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    status       VARCHAR(20),
    workspace_id BIGINT,
    created_at   TIMESTAMP(6),
    updated_at   TIMESTAMP(6),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT       NOT NULL,
    role    VARCHAR(255),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

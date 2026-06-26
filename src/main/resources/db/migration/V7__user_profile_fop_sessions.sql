-- User profile extensions, FOP profile, and session management

ALTER TABLE users
    ADD COLUMN first_name VARCHAR(100),
    ADD COLUMN last_name VARCHAR(100),
    ADD COLUMN phone VARCHAR(30);

UPDATE users
SET first_name = CASE
        WHEN position(' ' IN name) > 0 THEN trim(substring(name FROM 1 FOR position(' ' IN name) - 1))
        ELSE trim(name)
    END,
    last_name = CASE
        WHEN position(' ' IN name) > 0 THEN trim(substring(name FROM position(' ' IN name) + 1))
        ELSE ''
    END
WHERE first_name IS NULL;

UPDATE users SET last_name = '' WHERE last_name IS NULL;

CREATE TABLE fop_profiles (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT        NOT NULL,
    fop_group          SMALLINT      NOT NULL DEFAULT 2,
    tax_system         VARCHAR(30)   NOT NULL DEFAULT 'SINGLE_TAX',
    vat_payer          BOOLEAN       NOT NULL DEFAULT FALSE,
    tax_rate           NUMERIC(6, 4),
    registration_date  DATE,
    region             VARCHAR(100),
    main_kved          VARCHAR(20),
    main_kved_name     VARCHAR(255),
    kved_codes         JSONB         NOT NULL DEFAULT '[]'::jsonb,
    created_at         TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT uk_fop_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_fop_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_fop_profiles_user_id ON fop_profiles (user_id);

CREATE TABLE user_sessions (
    id                 VARCHAR(36)   PRIMARY KEY,
    user_id            BIGINT        NOT NULL,
    refresh_token_hash VARCHAR(64)   NOT NULL,
    device_label       VARCHAR(255),
    browser            VARCHAR(100),
    ip_address         VARCHAR(45),
    user_agent         TEXT,
    login_at           TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    last_activity_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at         TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);
CREATE INDEX idx_user_sessions_refresh_hash ON user_sessions (refresh_token_hash);

-- Initial schema for Flowiq Backend

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(100)  NOT NULL,
    password        VARCHAR(255)  NOT NULL,
    name            VARCHAR(100)  NOT NULL,
    company         VARCHAR(100),
    role            VARCHAR(20)   NOT NULL,
    is_active       BOOLEAN       NOT NULL,
    email_verified  BOOLEAN       NOT NULL,
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT        NOT NULL,
    type             VARCHAR(20)   NOT NULL,
    amount           NUMERIC(15, 2) NOT NULL,
    category         VARCHAR(100)  NOT NULL,
    description      VARCHAR(255),
    transaction_date DATE          NOT NULL,
    created_at       TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at       TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_transaction_date ON transactions (transaction_date);
CREATE INDEX idx_transactions_user_id_transaction_date ON transactions (user_id, transaction_date);

CREATE TABLE chat_conversations (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT fk_chat_conversations_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_chat_conversations_user_id ON chat_conversations (user_id);

CREATE TABLE chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT       NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_chat_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id)
);

CREATE INDEX idx_chat_messages_conversation_id ON chat_messages (conversation_id);

CREATE TABLE import_jobs (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    file_size      BIGINT       NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    rows_processed INTEGER      NOT NULL,
    rows_imported  INTEGER      NOT NULL,
    errors_count   INTEGER      NOT NULL,
    bank_format    VARCHAR(50),
    created_at     TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_import_jobs_user_id ON import_jobs (user_id);

CREATE TABLE report_jobs (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    report_type  VARCHAR(30)  NOT NULL,
    format       VARCHAR(10)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_size    BIGINT       NOT NULL,
    period_from  DATE         NOT NULL,
    period_to    DATE         NOT NULL,
    file_content BYTEA,
    created_at   TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_report_jobs_user_id ON report_jobs (user_id);

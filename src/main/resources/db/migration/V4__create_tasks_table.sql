CREATE TABLE tasks (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT        NOT NULL,
    title              VARCHAR(255)  NOT NULL,
    description        VARCHAR(1000),
    type               VARCHAR(20)   NOT NULL,
    priority           VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    status             VARCHAR(20)   NOT NULL DEFAULT 'TODO',
    due_date           DATE,
    completed_at       TIMESTAMP(6) WITHOUT TIME ZONE,
    deduplication_key  VARCHAR(255),
    created_at         TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE UNIQUE INDEX uk_tasks_user_dedup ON tasks (user_id, deduplication_key)
    WHERE deduplication_key IS NOT NULL;

CREATE INDEX idx_tasks_user_id ON tasks (user_id);
CREATE INDEX idx_tasks_user_status ON tasks (user_id, status);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
CREATE INDEX idx_tasks_user_due_date ON tasks (user_id, due_date);

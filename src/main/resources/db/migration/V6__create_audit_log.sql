-- V6: Append-only audit log for security and financial compliance (ADR-013)

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   BIGINT,
    actor_email     VARCHAR(100),
    actor_role      VARCHAR(20),
    event_type      VARCHAR(50)  NOT NULL,
    outcome         VARCHAR(20)  NOT NULL,
    http_method     VARCHAR(10),
    request_path    VARCHAR(255),
    resource_type   VARCHAR(50),
    resource_id     BIGINT,
    ip_address      INET,
    user_agent      VARCHAR(512),
    correlation_id  VARCHAR(64),
    metadata        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_log_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_actor_created
    ON audit_log (actor_user_id, created_at DESC);

CREATE INDEX idx_audit_log_event_created
    ON audit_log (event_type, created_at DESC);

CREATE INDEX idx_audit_log_resource
    ON audit_log (resource_type, resource_id)
    WHERE resource_id IS NOT NULL;

CREATE INDEX idx_audit_log_created_at
    ON audit_log (created_at);

COMMENT ON TABLE audit_log IS 'Append-only security and financial audit trail. No UPDATE/DELETE via application.';

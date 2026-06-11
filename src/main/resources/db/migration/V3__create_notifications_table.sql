CREATE TABLE notifications (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT        NOT NULL,
    title             VARCHAR(255)  NOT NULL,
    message           VARCHAR(1000) NOT NULL,
    type              VARCHAR(20)   NOT NULL,
    severity          VARCHAR(20)   NOT NULL,
    channel           VARCHAR(20)   NOT NULL DEFAULT 'IN_APP',
    is_read           BOOLEAN       NOT NULL DEFAULT false,
    action_url        VARCHAR(255),
    deduplication_key VARCHAR(255)  NOT NULL,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    read_at           TIMESTAMP(6) WITHOUT TIME ZONE,
    expires_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT uk_notifications_user_dedup UNIQUE (user_id, deduplication_key)
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_id_is_read ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);

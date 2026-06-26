CREATE TABLE notification_preferences (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT        NOT NULL,
    notification_type VARCHAR(50)   NOT NULL,
    channel           VARCHAR(20)   NOT NULL,
    enabled           BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT uk_notification_preferences_user_type_channel
        UNIQUE (user_id, notification_type, channel),
    CONSTRAINT fk_notification_preferences_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences (user_id);

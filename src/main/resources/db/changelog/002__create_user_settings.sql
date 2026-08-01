--liquibase formatted sql

--changeset matigfv:3
CREATE TABLE user_settings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    setting_key    VARCHAR(50)  NOT NULL,
    setting_value  BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_setting UNIQUE (user_id, setting_key)
);
--rollback DROP TABLE user_settings;

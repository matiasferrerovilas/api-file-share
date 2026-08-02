--liquibase formatted sql

--changeset matigfv:4
CREATE TABLE app_file_shares (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id      BIGINT       NOT NULL,
    api_name     VARCHAR(100) NOT NULL,
    permission   VARCHAR(20)  NOT NULL,
    created_by   BIGINT       NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_file_shares_file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE,
    CONSTRAINT chk_app_file_shares_permission CHECK (permission IN ('READ', 'WRITE', 'READ_WRITE')),
    CONSTRAINT uq_app_file_shares UNIQUE (file_id, api_name)
);
--rollback DROP TABLE app_file_shares;

--changeset matigfv:5
CREATE INDEX idx_app_file_shares_file_id ON app_file_shares (file_id);
--rollback DROP INDEX idx_app_file_shares_file_id ON app_file_shares;

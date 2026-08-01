--liquibase formatted sql

--changeset matigfv:1
CREATE TABLE files (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id    BIGINT,
    owner_id     BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(20) NOT NULL,
    size         BIGINT,
    location     VARCHAR(1024),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_files_parent FOREIGN KEY (parent_id) REFERENCES files (id) ON DELETE CASCADE,
    CONSTRAINT chk_files_type CHECK (type IN ('FOLDER', 'FILE'))
);
--rollback DROP TABLE files;

--changeset matigfv:2
CREATE INDEX idx_files_owner_id ON files (owner_id);
CREATE INDEX idx_files_parent_id ON files (parent_id);
CREATE INDEX idx_files_workspace_id ON files (workspace_id);
--rollback DROP INDEX idx_files_owner_id ON files;
--rollback DROP INDEX idx_files_parent_id ON files;
--rollback DROP INDEX idx_files_workspace_id ON files;

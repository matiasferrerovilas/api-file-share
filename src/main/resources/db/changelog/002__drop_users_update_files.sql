--liquibase formatted sql

--changeset matigfv:4
ALTER TABLE files DROP CONSTRAINT fk_files_owner;
--rollback ALTER TABLE files ADD CONSTRAINT fk_files_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

--changeset matigfv:5
DROP TABLE users;
--rollback CREATE TABLE users (id BIGSERIAL PRIMARY KEY, email VARCHAR(255) UNIQUE, given_name VARCHAR(255), family_name VARCHAR(255), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, is_first_login BOOLEAN NOT NULL DEFAULT TRUE, user_type VARCHAR(255) NOT NULL, has_seen_tour BOOLEAN NOT NULL DEFAULT FALSE);

--changeset matigfv:6
ALTER TABLE files RENAME COLUMN ubicacion TO location;
--rollback ALTER TABLE files RENAME COLUMN location TO ubicacion;

--changeset matigfv:7
DELETE FROM files;
--rollback

--changeset matigfv:8
ALTER TABLE files ADD COLUMN workspace_id BIGINT NOT NULL;
--rollback ALTER TABLE files DROP COLUMN workspace_id;

--changeset matigfv:9
CREATE INDEX idx_files_workspace_id ON files (workspace_id);
--rollback DROP INDEX idx_files_workspace_id;

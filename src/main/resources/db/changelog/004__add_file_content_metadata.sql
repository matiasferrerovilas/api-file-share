--liquibase formatted sql

--changeset matigfv:4
ALTER TABLE files ADD COLUMN content_type VARCHAR(255);
ALTER TABLE files ADD COLUMN checksum VARCHAR(64);
--rollback ALTER TABLE files DROP COLUMN content_type;
--rollback ALTER TABLE files DROP COLUMN checksum;

ALTER TABLE short_url ADD COLUMN member_id BIGINT NULL;
CREATE INDEX idx_short_url_member_id_id ON short_url (member_id, id);

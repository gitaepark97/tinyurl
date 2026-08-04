ALTER TABLE click_event DROP INDEX idx_click_event_short_url_id;
ALTER TABLE click_event ADD INDEX idx_click_event_short_url_id_id (short_url_id, id);

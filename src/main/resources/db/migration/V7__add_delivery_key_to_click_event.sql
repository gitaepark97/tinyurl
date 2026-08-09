ALTER TABLE click_event
    ADD COLUMN delivery_key VARCHAR(64) NULL,
    ADD UNIQUE INDEX uk_click_event_delivery_key (delivery_key);

CREATE TABLE click_event
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url_id BIGINT        NOT NULL,
    ip_address   VARCHAR(45),
    user_agent   VARCHAR(512),
    referer      VARCHAR(2048),
    clicked_at   DATETIME      NOT NULL,
    INDEX idx_click_event_short_url_id (short_url_id)
);

CREATE TABLE click_count
(
    short_url_id BIGINT PRIMARY KEY,
    count        BIGINT NOT NULL DEFAULT 0
);

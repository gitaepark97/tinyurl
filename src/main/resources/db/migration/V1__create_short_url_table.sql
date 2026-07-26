CREATE TABLE short_url
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_key    VARCHAR(8) COLLATE utf8mb4_0900_as_cs NOT NULL,
    original_url VARCHAR(2048) NOT NULL,
    expires_at   DATETIME     NOT NULL,
    created_at   DATETIME     NOT NULL,
    CONSTRAINT uk_short_url_short_key UNIQUE (short_key)
);

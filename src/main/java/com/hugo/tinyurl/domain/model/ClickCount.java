package com.hugo.tinyurl.domain.model;

public record ClickCount(
    Long shortUrlId,
    long count
) {
}

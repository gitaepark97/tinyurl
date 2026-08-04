package com.hugo.tinyurl.support.page;

import com.hugo.tinyurl.support.exception.BusinessException;
import com.hugo.tinyurl.support.exception.ErrorCode;

public record PageParam(
    Long cursor,
    Integer size
) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long INITIAL_CURSOR = Long.MAX_VALUE;

    public PageParam {
        if (size == null) {
            size = DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    public long cursorOrInitial() {
        return cursor != null ? cursor : INITIAL_CURSOR;
    }

}

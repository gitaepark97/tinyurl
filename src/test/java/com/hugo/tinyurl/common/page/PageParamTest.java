package com.hugo.tinyurl.common.page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hugo.tinyurl.common.exception.BusinessException;
import com.hugo.tinyurl.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class PageParamTest {

    @Test
    void defaultsSizeTo20WhenNull() {
        PageParam pageParam = new PageParam(null, null);

        assertThat(pageParam.size()).isEqualTo(20);
    }

    @Test
    void acceptsBoundarySizes() {
        assertThat(new PageParam(null, 1).size()).isEqualTo(1);
        assertThat(new PageParam(null, 100).size()).isEqualTo(100);
    }

    @Test
    void rejectsSizeBelowMinimum() {
        assertThatThrownBy(() -> new PageParam(null, 0))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void rejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> new PageParam(null, 101))
            .isInstanceOf(BusinessException.class)
            .extracting(e -> ((BusinessException) e).errorCode())
            .isEqualTo(ErrorCode.INVALID_INPUT);
    }

}

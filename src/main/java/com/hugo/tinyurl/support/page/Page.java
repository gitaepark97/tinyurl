package com.hugo.tinyurl.support.page;

import java.util.List;
import java.util.function.Function;

public record Page<T>(
    List<T> content,
    boolean hasNext
) {

    public static <T> Page<T> of(List<T> content, boolean hasNext) {
        return new Page<>(content, hasNext);
    }

    public <R> Page<R> map(Function<? super T, R> mapper) {
        return new Page<>(content.stream().map(mapper).toList(), hasNext);
    }

}

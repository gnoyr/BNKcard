package com.bnk.global.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {

    private final List<T> content;
    private final long totalCount;
    private final int totalPages;
    private final int currentPage;
    private final int pageSize;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public static <T> PageResponse<T> of(List<T> content, long totalCount, int page, int size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalCount / size);
        return PageResponse.<T>builder()
                .content(content)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .currentPage(page)
                .pageSize(size)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }
}

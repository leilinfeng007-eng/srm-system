package com.srm.common.api;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        long totalPages) {

    public PageResult {
        items = List.copyOf(items);
        if (page < 1 || pageSize < 1 || total < 0) {
            throw new IllegalArgumentException("Invalid page metadata");
        }
        long expectedPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        if (totalPages != expectedPages) {
            throw new IllegalArgumentException("totalPages does not match total and pageSize");
        }
    }

    public static <T> PageResult<T> of(List<T> items, int page, int pageSize, long total) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(items, page, pageSize, total, totalPages);
    }
}


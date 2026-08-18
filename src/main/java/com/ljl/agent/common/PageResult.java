package com.ljl.agent.common;

import java.util.List;

public class PageResult<T> {

    private final List<T> records;
    private final long total;
    private final int page;
    private final int size;
    private final long totalPages;

    public PageResult(
            List<T> records,
            long total,
            int page,
            int size) {

        if (total < 0) {
            throw new IllegalArgumentException("total 不能小于 0");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page 不能小于 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size 不能小于 1");
        }

        this.records = records == null ? List.of() : List.copyOf(records);
        this.total = total;
        this.page = page;
        this.size = size;

        this.totalPages =
                total == 0
                        ? 0
                        : (total - 1) / size + 1;
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalPages() {
        return totalPages;
    }
}

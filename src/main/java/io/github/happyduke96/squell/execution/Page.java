package io.github.happyduke96.squell.execution;

import java.util.List;

/// One page of an offset-paginated result: the matching rows for that page plus the total count
/// of rows across every page, fetched together by [SelectStep#fetchPage].
public record Page<T>(List<T> content, long totalElements, int pageNumber, int pageSize) {

    public boolean hasNext() {
        return (long) (pageNumber + 1) * pageSize < totalElements;
    }

    public int totalPages() {
        return pageSize <= 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
    }
}

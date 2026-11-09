package io.github.happyduke96.squell.execution;

import java.util.List;

/**
 * One page of an offset-paginated result, produced by {@code SelectStep#fetchPage}.
 *
 * @param content the rows of this page
 * @param totalElements the row count across all pages
 * @param pageNumber the 0-based page index
 * @param pageSize the requested page size
 * @param <T> the entity type
 */
public record Page<T>(List<T> content, long totalElements, int pageNumber, int pageSize) {

    public boolean hasNext() {
        return (long) (pageNumber + 1) * pageSize < totalElements;
    }

    public int totalPages() {
        return pageSize <= 0 ? 0 : (int) Math.ceil((double) totalElements / pageSize);
    }
}

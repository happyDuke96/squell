package io.github.happyduke96.squell.execution;

import java.util.List;

/**
 * One page of a keyset-paginated result, produced by {@code SelectStep#fetchSlice}.
 *
 * <p>It needs neither {@code COUNT(*)} nor {@code OFFSET}: {@code hasNext} is derived by fetching
 * one row past the limit.
 *
 * @param content the rows of this slice
 * @param hasNext whether more rows follow
 * @param <T> the entity type
 */
public record Slice<T>(List<T> content, boolean hasNext) {
}

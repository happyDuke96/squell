package io.github.happyduke96.squell.execution;

import java.util.List;

/// One page of a keyset-paginated result: no `COUNT(*)`, no `OFFSET` — [SelectStep#fetchSlice]
/// fetches one extra row past `limit` to derive [#hasNext] instead. Pair with `where(cursorField
/// .gt(lastSeenValue))` and `orderBy(cursorField)` to keep paging through large tables.
public record Slice<T>(List<T> content, boolean hasNext) {
}

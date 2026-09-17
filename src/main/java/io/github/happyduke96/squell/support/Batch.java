package io.github.happyduke96.squell.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Groups a flat list of children under their parents by a shared key — avoids an N+1 query when
/// fetching parents and children separately and joining them in memory.
public final class Batch<P, K, C> {

    private final List<P> parents;
    private final Function<P, K> parentKey;
    private final List<C> children;
    private final Function<C, K> childKey;

    public Batch(List<P> parents, Function<P, K> parentKey, List<C> children, Function<C, K> childKey) {
        this.parents = Collections.unmodifiableList(new ArrayList<>(parents));
        this.parentKey = parentKey;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
        this.childKey = childKey;
    }

    public Map<P, List<C>> groupBy() {
        Map<K, List<C>> byKey = children.stream().collect(Collectors.groupingBy(childKey));
        Map<P, List<C>> result = new LinkedHashMap<>();
        for (P parent : parents) {
            result.put(parent, byKey.getOrDefault(parentKey.apply(parent), List.of()));
        }
        return result;
    }
}

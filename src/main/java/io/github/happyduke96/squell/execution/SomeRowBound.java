package io.github.happyduke96.squell.execution;

import java.util.List;

public final class SomeRowBound implements RowBound {

    private final String keyword;
    private final int value;

    public SomeRowBound(String keyword, int value) {
        this.keyword = keyword;
        this.value = value;
    }

    @Override
    public String sql() {
        return " " + keyword + " ?";
    }

    @Override
    public List<Object> values() {
        return List.of(value);
    }
}

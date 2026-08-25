package io.github.happyduke96.squell.execution;

import java.util.List;

public final class NoRowBound implements RowBound {

    @Override
    public String sql() {
        return "";
    }

    @Override
    public List<Object> values() {
        return List.of();
    }
}

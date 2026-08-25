package io.github.happyduke96.squell.execution;

import java.util.List;

public sealed interface RowBound permits NoRowBound, SomeRowBound {

    String sql();

    List<Object> values();
}

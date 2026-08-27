package io.github.happyduke96.squell.converter.mysql;

import io.github.happyduke96.squell.converter.Converter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SetConverter implements Converter<Set<String>, String> {

    @Override
    public String toSql(Set<String> value) {
        return value == null ? null : String.join(",", value);
    }

    @Override
    public Set<String> fromSql(String sqlValue) {
        if (sqlValue == null || sqlValue.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(sqlValue.split(",")));
    }
}

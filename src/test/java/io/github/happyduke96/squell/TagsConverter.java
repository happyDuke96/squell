package io.github.happyduke96.squell;

import io.github.happyduke96.squell.converter.Converter;

import java.util.Arrays;
import java.util.List;

/// Converts a comma-joined `VARCHAR` column to/from `List<String>`.
public final class TagsConverter implements Converter<List<String>, String> {

    @Override
    public String toSql(List<String> value) {
        return String.join(",", value);
    }

    @Override
    public List<String> fromSql(String sqlValue) {
        return sqlValue == null || sqlValue.isEmpty() ? List.of() : Arrays.asList(sqlValue.split(","));
    }
}

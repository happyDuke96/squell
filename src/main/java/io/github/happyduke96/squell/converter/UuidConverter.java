package io.github.happyduke96.squell.converter;


import java.util.UUID;

public final class UuidConverter implements Converter<UUID, String> {

    @Override
    public String toSql(UUID value) {
        return value == null ? null : value.toString();
    }

    @Override
    public UUID fromSql(String sqlValue) {
        return sqlValue == null ? null : UUID.fromString(sqlValue);
    }
}

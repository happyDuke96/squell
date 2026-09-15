package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks the primary key. Implies non-null and unique; a numeric key also rejects negative values.
///
/// At most one method may carry it — squell doesn't model composite primary keys. For a table
/// with no single-column key (a join table, a composite key), omit `@Id` entirely: the generated
/// `Table` then implements plain `Table<T>` instead of `DefaultTable<T, ID>`, so it simply has no
/// `id()`/`byId()` — this is the supported way to model such a table, not a workaround.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Id {
}

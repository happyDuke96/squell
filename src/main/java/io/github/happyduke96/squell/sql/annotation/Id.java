package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks the single-column primary key — non-null, unique, non-negative if numeric. Composite keys
/// aren't modeled: omit `@Id` and the table has no `id()`/`byId()`.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Id {
}

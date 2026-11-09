package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// The database assigns this column's value; `create()` omits it. Declare the plain type — it's
/// `0`/`false`/`null` until read back (`executeAndReturn()`, `select`); `Optional<...>` won't compile.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface GeneratedValue {
}

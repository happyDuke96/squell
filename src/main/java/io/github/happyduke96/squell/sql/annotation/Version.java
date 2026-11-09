package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Optimistic-lock counter (`int`/`long`/`Integer`/`Long`), starts at `0`, omitted from `create()`.
/// Pair with `UpdateStep#whereVersion`; at most one per entity, never the `@Id`.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Version {
}

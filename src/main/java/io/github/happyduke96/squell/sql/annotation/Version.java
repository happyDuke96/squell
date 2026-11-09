package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the optimistic-lock counter ({@code int}, {@code long}, {@code Integer} or {@code Long}).
 *
 * <p>It starts at {@code 0} and is omitted from {@code create()}. Use it with
 * {@code UpdateStep#whereVersion}; at most one per entity, never the {@code @Id}.
 *
 * @since 0.3.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Version {
}

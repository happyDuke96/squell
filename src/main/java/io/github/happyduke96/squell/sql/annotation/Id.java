package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the single-column primary key: not null, unique and, if numeric, non-negative.
 *
 * <p>Composite keys are not modeled: omit {@code @Id} and the table has no {@code id()} or
 * {@code byId()}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Id {
}

package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a column whose value the database assigns; {@code create()} omits it.
 *
 * <p>Declare the plain type: until read back the value is {@code 0} or {@code false} for a
 * primitive and {@code null} otherwise. {@code Optional} is rejected at compile time.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface GeneratedValue {
}

package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Overrides the column name and/or declares constraints checked by `Client#validateSchema`.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Column {

    String value() default "";

    boolean nonNull() default false;

    boolean unique() default false;
}

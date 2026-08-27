package io.github.happyduke96.squell.sql.annotation;

import io.github.happyduke96.squell.converter.Converter;
import io.github.happyduke96.squell.converter.NoConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Routes a field through a `Converter` on the way to/from the column. Naming a class with a
/// no-arg constructor here (instead of leaving the default) lets the generated `Table` build it
/// itself, so callers don't have to pass converter instances in.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Convert {

    Class<? extends Converter> value() default NoConverter.class;
}

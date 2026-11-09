package io.github.happyduke96.squell.sql.annotation;

import io.github.happyduke96.squell.converter.Converter;
import io.github.happyduke96.squell.converter.NoConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Routes a field through a {@code Converter} to and from its column.
 *
 * <p>Naming a converter class with a no-arg constructor lets the generated table instantiate it
 * itself.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Convert {

    Class<? extends Converter> value() default NoConverter.class;
}

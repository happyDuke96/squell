package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// On a `package-info.java`, generates a `Tables` class listing every entity in the package whose
/// `Table` has a no-arg constructor.
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateTableRegistry {
}

package io.github.happyduke96.squell.sql.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// The database assigns this column's value (e.g. `DEFAULT gen_random_uuid()`). The generated
/// `create()` omits it, and it's excluded from inserted columns.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface GeneratedValue {
}

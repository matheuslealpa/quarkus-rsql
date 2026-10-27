package io.github.matheuslealpa.rsql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a persistent attribute from RSQL filters, for example a password hash or a token.
 * <p>
 * Annotate the field or its getter: the attribute is excluded either way, whichever of them
 * Hibernate ORM uses to access it.
 * <p>
 * A filter that references the attribute, directly or through a path such as
 * {@code owner.passwordHash}, is rejected as if the attribute didn't exist, so that its existence
 * isn't revealed.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface RsqlIgnore {

}

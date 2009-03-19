package org.horizon.factories.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mechanism for specifying the name of components to retrieve
 *
 * @author Manik Surtani
 * @since 1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentName {
   String value();
}

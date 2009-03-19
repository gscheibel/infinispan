package org.horizon.factories.scopes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines the scope of a component in a cache system.  If not specified, components default to the {@link
 * Scopes#EITHER} scope.
 *
 * @author Manik Surtani
 * @see Scopes
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {
   Scopes value();
}

package org.horizon.factories;

/**
 * A specialized type of component factory that knows how to create named components, identified with the {@link
 * org.horizon.factories.annotations.ComponentName} annotation on the classes requested in {@link
 * org.horizon.factories.annotations.Inject} annotated methods.
 *
 * @author Manik Surtani
 * @since 1.0
 */
public abstract class NamedComponentFactory extends AbstractComponentFactory {

   public <T> T construct(Class<T> componentType) {

      // by default, use the FQCN of the component type.
      return construct(componentType, componentType.getName());
   }

   /**
    * Constructs a component.
    *
    * @param componentType type of component
    * @return a component
    */
   public abstract <T> T construct(Class<T> componentType, String componentName);
}

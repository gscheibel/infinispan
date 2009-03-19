package org.horizon.loader.jdbc.stringbased;

/**
 * Used for testing jdbc cache stores.
 *
 * @author Mircea.Markus@jboss.com
 */
public class PersonKey2StringMapper implements Key2StringMapper {
   public boolean isSupportedType(Class keyType) {
      return keyType == Person.class;
   }

   public String getStringMapping(Object key) {
      Person person = (Person) key;
      return person.getName() + "_" + person.getSurname() + "_" + person.getAge();
   }
}

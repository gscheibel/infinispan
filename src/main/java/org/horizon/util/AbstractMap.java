package org.horizon.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Similar to the JDK's AbstractMap, this provides common functionality for custom map implementations.  Unlike JDK's
 * AbstractMap, there is no support for null keys.
 *
 * @author Manik Surtani
 * @since 1.0
 */
public abstract class AbstractMap<K, V> implements Map<K, V> {
   // views
   protected transient Set<Map.Entry<K, V>> entrySet = null;
   protected transient Set<K> keySet = null;
   protected transient Collection<V> values = null;


   // The normal bit spreader...
   protected static final int hash(Object key) {
      int h = key.hashCode();
      h ^= (h >>> 20) ^ (h >>> 12);
      return h ^ (h >>> 7) ^ (h >>> 4);
   }

   protected static final boolean eq(Object o1, Object o2) {
      return o1 == o2 || (o1 != null && o1.equals(o2));
   }

   protected final void assertKeyNotNull(Object key) {
      if (key == null) throw new NullPointerException("Null keys are not supported!");
   }

   protected static class SimpleEntry<K, V> implements Map.Entry<K, V> {
      private K key;
      private V value;

      SimpleEntry(K key, V value) {
         this.key = key;
         this.value = value;
      }

      SimpleEntry(Map.Entry<K, V> entry) {
         this.key = entry.getKey();
         this.value = entry.getValue();
      }

      public K getKey() {
         return key;
      }

      public V getValue() {
         return value;
      }

      public V setValue(V value) {
         V old = this.value;
         this.value = value;
         return old;
      }

      public boolean equals(Object o) {
         if (this == o)
            return true;

         if (!(o instanceof Map.Entry))
            return false;
         Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
         return eq(key, e.getKey()) && eq(value, e.getValue());
      }

      public int hashCode() {
         return hash(key) ^
               (value == null ? 0 : hash(value));
      }

      public String toString() {
         return getKey() + "=" + getValue();
      }
   }
}

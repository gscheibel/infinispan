/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.horizon.commands.write;

import org.horizon.commands.Visitor;
import org.horizon.context.InvocationContext;
import org.horizon.logging.Log;
import org.horizon.logging.LogFactory;
import org.horizon.notifications.cachelistener.CacheNotifier;

import java.util.Arrays;


/**
 * Removes an entry from memory - never removes the entry.
 *
 * @author Mircea.Markus@jboss.com
 * @since 1.0
 */
public class InvalidateCommand extends RemoveCommand {
   public static final int METHOD_ID = 47;
   private static final Log log = LogFactory.getLog(InvalidateCommand.class);
   private static final boolean trace = log.isTraceEnabled();
   private Object[] keys;

   public InvalidateCommand() {
   }

   public InvalidateCommand(CacheNotifier notifier, Object... keys) {
      this.keys = keys;
      this.notifier = notifier;
   }

   /**
    * Performs an invalidation on a specified entry
    *
    * @param ctx invocation context
    * @return null
    */
   public Object perform(InvocationContext ctx) throws Throwable {
      if (trace) log.trace("Invalidating keys:" + Arrays.toString(keys));
      for (Object key : keys) {
         this.key = key;
         super.perform(ctx);
      }
      return null;
   }

   @Override
   protected void notify(InvocationContext ctx, Object value, boolean isPre) {
      notifier.notifyCacheEntryInvalidated(key, isPre, ctx);
   }

   public byte getCommandId() {
      return METHOD_ID;
   }

   @Override
   public String toString() {
      return "InvalidateCommand{" +
            "keys=" + Arrays.toString(keys) +
            '}';
   }

   @Override
   public Object[] getParameters() {
      if (keys == null || keys.length == 0) {
         return new Object[]{0};
      } else if (keys.length == 1) {
         return new Object[]{1, keys[0]};
      } else {
         Object[] retval = new Object[keys.length + 1];
         retval[0] = keys.length;
         System.arraycopy(keys, 0, retval, 1, keys.length);
         return retval;
      }
   }

   @Override
   public void setParameters(int commandId, Object[] args) {
      int size = (Integer) args[0];
      keys = new Object[size];
      if (size == 1) {
         keys[0] = args[1];
      } else if (size > 0) {
         System.arraycopy(args, 1, keys, 0, size);
      }
   }

   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
      return visitor.visitInvalidateCommand(ctx, this);
   }

   @Override
   public Object getKey() {
      throw new UnsupportedOperationException("Not supported.  Use getKeys() instead.");
   }

   public Object[] getKeys() {
      return keys;
   }
}
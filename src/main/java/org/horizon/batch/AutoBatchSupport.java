/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.horizon.batch;

import net.jcip.annotations.NotThreadSafe;
import org.horizon.config.Configuration;
import org.horizon.config.ConfigurationException;

/**
 * Enables for automatic batching.
 *
 * @author Manik Surtani (<a href="mailto:manik AT jboss DOT org">manik AT jboss DOT org</a>)
 * @since 1.0
 */
@NotThreadSafe
public abstract class AutoBatchSupport {
   protected BatchContainer batchContainer;

   protected void assertBatchingSupported(Configuration c) {
      if (!c.isInvocationBatchingEnabled())
         throw new ConfigurationException("Invocation batching not enabled in current configuration!  Please use the <invocationBatching /> element.");
   }

   protected void startAtomic() {
      batchContainer.startBatch(true);
   }

   protected void endAtomic() {
      batchContainer.endBatch(true, true);
   }
}

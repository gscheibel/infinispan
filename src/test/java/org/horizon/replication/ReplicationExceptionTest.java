/*
 *
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.horizon.replication;

import org.horizon.AdvancedCache;
import org.horizon.Cache;
import org.horizon.commands.VisitableCommand;
import org.horizon.config.Configuration;
import org.horizon.context.InvocationContext;
import org.horizon.interceptors.base.CommandInterceptor;
import org.horizon.lock.IsolationLevel;
import org.horizon.lock.TimeoutException;
import org.horizon.test.MultipleCacheManagersTest;
import org.horizon.test.TestingUtil;
import org.horizon.transaction.DummyTransactionManagerLookup;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.List;

@Test(groups = "functional", testName = "replication.ReplicationExceptionTest")
public class ReplicationExceptionTest extends MultipleCacheManagersTest {
   private AdvancedCache cache1, cache2;

   protected void createCacheManagers() throws Throwable {
      Configuration configuration = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      configuration.setIsolationLevel(IsolationLevel.REPEATABLE_READ);

      configuration.setTransactionManagerLookupClass(DummyTransactionManagerLookup.class.getName());
      configuration.setLockAcquisitionTimeout(5000);

      List<Cache> caches = createClusteredCaches(2, "replicatinExceptionTest", configuration);

      cache1 = caches.get(0).getAdvancedCache();
      cache2 = caches.get(1).getAdvancedCache();
   }

   private TransactionManager beginTransaction() throws SystemException, NotSupportedException {
      TransactionManager mgr = TestingUtil.getTransactionManager(cache1);
      mgr.begin();
      return mgr;
   }

   public void testNonSerializableRepl() throws Exception {
      try {
         cache1.put("test", new ContainerData());

         // We should not come here.
         assertNotNull("NonSerializableData should not be null on cache2", cache2.get("test"));
      }
      catch (RuntimeException runtime) {
         Throwable t = runtime.getCause();
         if (t instanceof NotSerializableException) {
            System.out.println("received NotSerializableException - as expected");
         } else {
            throw runtime;
         }
      }
   }

   public void testNonSerializableReplWithTx() throws Exception {
      TransactionManager tm;

      try {
         tm = beginTransaction();
         cache1.put("test", new ContainerData());
         tm.commit();

         // We should not come here.
         assertNotNull("NonSerializableData should not be null on cache2", cache2.get("test"));
      }
      catch (RollbackException rollback) {
         System.out.println("received RollbackException - as expected");
      }
      catch (Exception e) {
         // We should also examine that it is indeed throwing a NonSerilaizable exception.
         fail(e.toString());
      }
   }

   @Test(groups = "functional", expectedExceptions = {TimeoutException.class})
   public void testSyncReplTimeout() {
      cache2.addInterceptor(new CommandInterceptor() {
         @Override
         protected Object handleDefault(InvocationContext ctx, VisitableCommand cmd) throws Throwable {
            // Add a delay
            Thread.sleep(100);
            return super.handleDefault(ctx, cmd);
         }
      }, 0);

      cache1.getConfiguration().setSyncReplTimeout(1);
      cache2.getConfiguration().setSyncReplTimeout(1);
      TestingUtil.blockUntilViewsReceived(10000, cache1, cache2);

      cache1.put("k", "v");
   }

   @Test(groups = "functional", expectedExceptions = {TimeoutException.class})
   public void testLockAcquisitionTimeout() throws Exception {
      cache2.getConfiguration().setLockAcquisitionTimeout(1);
      TestingUtil.blockUntilViewsReceived(10000, cache1, cache2);

      // get a lock on cache 2 and hold on to it.
      TransactionManager tm = TestingUtil.getTransactionManager(cache2);
      tm.begin();
      cache2.put("block", "block");
      tm.suspend();
      cache1.put("block", "v");
   }

   static class NonSerializabeData {
      int i;
   }

   static class ContainerData implements Serializable {
      int i;
      NonSerializabeData non_serializable_data;
      private static final long serialVersionUID = -8322197791060897247L;

      public ContainerData() {
         i = 99;
         non_serializable_data = new NonSerializabeData();
      }
   }
}

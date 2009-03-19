package org.horizon.api.batch;

import org.horizon.Cache;
import org.horizon.config.Configuration;
import org.horizon.manager.CacheManager;
import org.horizon.test.TestingUtil;
import org.horizon.transaction.DummyTransactionManagerLookup;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.transaction.TransactionManager;


@Test(groups = {"functional", "transaction"}, testName = "api.batch.BatchWithTMTest")
public class BatchWithTMTest extends AbstractBatchTest {

   CacheManager cm;

   @BeforeClass
   public void createCacheManager() {
      cm = TestingUtil.createLocalCacheManager();
   }

   @AfterClass
   public void destroyCacheManager() {
      TestingUtil.killCacheManagers(cm);
   }

   public void testBatchWithOngoingTM() throws Exception {
      Cache<String, String> cache = null;
      cache = createCache("testBatchWithOngoingTM");
      TransactionManager tm = TestingUtil.getTransactionManager(cache);
      tm.begin();
      cache.put("k", "v");
      cache.startBatch();
      cache.put("k2", "v2");
      tm.commit();

      assert "v".equals(cache.get("k"));
      assert "v2".equals(cache.get("k2"));

      cache.endBatch(false); // should be a no op
      assert "v".equals(cache.get("k"));
      assert "v2".equals(cache.get("k2"));
   }

   public void testBatchWithoutOngoingTMSuspension() throws Exception {
      Cache cache = createCache("testBatchWithoutOngoingTMSuspension");
      TransactionManager tm = TestingUtil.getTransactionManager(cache);
      assert tm.getTransaction() == null : "Should have no ongoing txs";
      cache.startBatch();
      cache.put("k", "v");
      assert tm.getTransaction() == null : "Should have no ongoing txs";
      cache.put("k2", "v2");

      assert getOnDifferentThread(cache, "k") == null;
      assert getOnDifferentThread(cache, "k2") == null;

      try {
         tm.commit(); // should have no effect
      }
      catch (Exception e) {
         // the TM may barf here ... this is OK.
      }

      assert tm.getTransaction() == null : "Should have no ongoing txs";

      assert getOnDifferentThread(cache, "k") == null;
      assert getOnDifferentThread(cache, "k2") == null;

      cache.endBatch(true); // should be a no op

      assert "v".equals(getOnDifferentThread(cache, "k"));
      assert "v2".equals(getOnDifferentThread(cache, "k2"));
   }

   public void testBatchRollback() throws Exception {
      Cache cache = createCache("testBatchRollback");
      cache.startBatch();
      cache.put("k", "v");
      cache.put("k2", "v2");

      assert getOnDifferentThread(cache, "k") == null;
      assert getOnDifferentThread(cache, "k2") == null;

      cache.endBatch(false);

      assert getOnDifferentThread(cache, "k") == null;
      assert getOnDifferentThread(cache, "k2") == null;
   }

   private Cache<String, String> createCache(String name) {
      Configuration c = new Configuration();
      c.setTransactionManagerLookupClass(DummyTransactionManagerLookup.class.getName());
      c.setInvocationBatchingEnabled(true);
      assert c.getTransactionManagerLookupClass() != null : "Should have a transaction manager lookup class attached!!";
      cm.defineCache(name, c);
      return cm.getCache(name);
   }
}

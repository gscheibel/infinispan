package org.horizon.profiling;

import org.horizon.Cache;
import org.horizon.config.Configuration;
import org.horizon.lock.IsolationLevel;
import org.horizon.logging.Log;
import org.horizon.logging.LogFactory;
import org.horizon.manager.CacheManager;
import org.horizon.manager.DefaultCacheManager;
import org.horizon.profiling.testinternals.Generator;
import org.horizon.profiling.testinternals.TaskRunner;
import org.horizon.test.TestingUtil;
import org.horizon.transaction.DummyTransactionManager;
import org.horizon.tree.Fqn;
import org.horizon.tree.TreeCache;
import org.horizon.tree.TreeCacheImpl;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test to use with a profiler to profile replication.  To be used in conjunction with ProfileSlaveTest.
 * <p/>
 * Typical usage pattern:
 * <p/>
 * 1.  Start a single test method in ProfileSlaveTest.  This will block until you kill it. 2.  Start the corresponding
 * test in this class, with the same name, in a different JVM, and attached to a profiler. 3.  Profile away!
 * <p/>
 *
 * @author Manik Surtani (<a href="mailto:manik@jboss.org">manik@jboss.org</a>)
 */
@Test(groups = "profiling", testName = "profiling.TreeProfileTest", enabled = false)
public class TreeProfileTest {
   Log log = LogFactory.getLog(TreeProfileTest.class);

   /**
      Test configuration options
    */
   protected static final long NUM_OPERATIONS = 1000000; // DURATION is replaced with a fixed number of operations instead.
   protected static final int NUM_THREADS = 25;
   protected static final int MAX_RANDOM_SLEEP_MILLIS = 1;
   protected static final int MAX_DEPTH = 3;
   protected static final int MAX_OVERALL_NODES = 2000;
   protected static final int WARMUP_LOOPS = 20000;

   protected static final boolean USE_SLEEP = false; // throttle generation a bit
   private CacheManager cacheManager;


   protected TreeCache cache;

   @BeforeMethod
   public void setUp() {
      Configuration cfg = new Configuration();
      cfg.setInvocationBatchingEnabled(true);
      cfg.setCacheMode(Configuration.CacheMode.LOCAL);
      cfg.setConcurrencyLevel(2000);
      cfg.setLockAcquisitionTimeout(120000);
      cfg.setIsolationLevel(IsolationLevel.READ_COMMITTED);
      cacheManager = new DefaultCacheManager(cfg);
      Cache c = cacheManager.getCache();
      cache = new TreeCacheImpl(c);
   }

   @AfterMethod
   public void tearDown() {
      TestingUtil.killTreeCaches(cache);
      TestingUtil.killCacheManagers(cacheManager);
   }

   private List<Fqn> fqns = new ArrayList<Fqn>(MAX_OVERALL_NODES);

   public void testLocalMode() throws Exception {
      runCompleteTest();
   }

   private void runCompleteTest() throws Exception {
      init();
      startup();
      warmup();
      doTest();

      // wait for user exit
      System.in.read();
   }

   /**
    * Thr following test phases can be profiled individually using triggers in JProfiler.
    */

   protected void init() {
      long startTime = System.currentTimeMillis();
      log.warn("Starting init() phase");
      fqns.clear();
      for (int i = 0; i < MAX_OVERALL_NODES; i++) {
         Fqn fqn;
         do {
            fqn = Generator.createRandomFqn(MAX_DEPTH);
         }
         while (fqns.contains(fqn));

         if (i % 100 == 0) log.warn("Generated " + i + " fqns");
         fqns.add(fqn);
      }
      System.gc();
      long duration = System.currentTimeMillis() - startTime;
      log.warn("Finished init() phase.  " + printDuration(duration));
   }

   protected void startup() {
      long startTime = System.currentTimeMillis();
      log.warn("Starting cache");
      cache.start();
      long duration = System.currentTimeMillis() - startTime;
      log.warn("Started cache.  " + printDuration(duration));
   }

   private void warmup() throws InterruptedException {
      long startTime = System.currentTimeMillis();
      TaskRunner runner = new TaskRunner(NUM_THREADS);
      log.warn("Starting warmup");
      // creates all the Fqns since this can be expensive and we don't really want to measure this (for now)
      for (final Fqn fqn : fqns) {
         runner.execute(new Runnable() {
            public void run() {
               try {
                  // this will create the necessary nodes.
                  cache.put(fqn, "key", Collections.emptyMap());
               }
               catch (Exception e) {
                  log.warn("Caught Exception", e);
               }
            }
         });
      }

      // loop through WARMUP_LOOPS gets and puts for JVM optimisation
      for (int i = 0; i < WARMUP_LOOPS; i++) {
         runner.execute(new Runnable() {
            public void run() {
               try {
                  Fqn fqn = Generator.getRandomElement(fqns);
                  DummyTransactionManager.getInstance().begin();
                  cache.get(fqn, "key");
                  DummyTransactionManager.getInstance().commit();
                  DummyTransactionManager.getInstance().begin();
                  cache.put(fqn, "key", "Value");
                  DummyTransactionManager.getInstance().commit();
                  DummyTransactionManager.getInstance().begin();
                  cache.remove(fqn, "key");
                  DummyTransactionManager.getInstance().commit();
               }
               catch (Exception e) {
                  log.warn("Caught Exception", e);
               }
            }
         });
      }

      runner.stop();

      long duration = System.currentTimeMillis() - startTime;
      log.warn("Finished warmup.  " + printDuration(duration));
      //cache.removeNode(Fqn.ROOT);
      cache.stop();

      startup();
   }

   private void doTest() throws Exception {
      TaskRunner runner = new TaskRunner(NUM_THREADS);

      log.warn("Starting test");
      int i;
      long print = NUM_OPERATIONS / 10;

      AtomicLong durationPuts = new AtomicLong();
      AtomicLong durationGets = new AtomicLong();
      AtomicLong durationRemoves = new AtomicLong();

      long stElapsed = System.nanoTime();
      for (i = 0; i < NUM_OPERATIONS; i++) {
         MyRunnable r = null;
         switch (i % 3) {
            case 0:
               r = new Putter(i, durationPuts);
               break;
            case 1:
               r = new Getter(i, durationGets);
               break;
            case 2:
               r = new Remover(i, durationRemoves);
               break;
         }
         if (i % print == 0)
            log.warn("processing iteration " + i);
         runner.execute(r);
//         if (USE_SLEEP) TestingUtil.sleepRandom(MAX_RANDOM_SLEEP_MILLIS);
         if (USE_SLEEP) TestingUtil.sleepThread(MAX_RANDOM_SLEEP_MILLIS);
      }
      log.warn("Finished generating runnables; awaiting executor completion");
      // wait for executors to complete!
      runner.stop();

      // wait up to 1 sec for each call?
      long elapsedTimeNanos = System.nanoTime() - stElapsed;

      log.warn("Finished test.  " + printDuration((long) toMillis(elapsedTimeNanos)));
      log.warn("Throughput: " + ((double) NUM_OPERATIONS * 1000 / toMillis(elapsedTimeNanos)) + " operations per second (roughly equal numbers of PUT, GET and REMOVE)");
      log.warn("Average GET time: " + printAvg(durationGets.get()));
      log.warn("Average PUT time: " + printAvg(durationPuts.get()));
      log.warn("Average REMOVE time: " + printAvg(durationRemoves.get()));
   }

   private String printAvg(long totalNanos) {
      double nOps = (double) (NUM_OPERATIONS / 3);
      double avg = ((double) totalNanos) / nOps;
      double avgMicros = avg / 1000;
      return avgMicros + " µs";
   }

   private double toMillis(long nanos) {
      return ((double) nanos / (double) 1000000);
   }

   enum Mode {
      PUT, GET, REMOVE
   }

   private abstract class MyRunnable implements Runnable {
      int id;
      Mode mode;
      AtomicLong duration;

      public void run() {
         Fqn fqn = Generator.getRandomElement(fqns);
         long d = 0, st = 0;
         try {
            switch (mode) {
               case PUT:
                  Object value = Generator.getRandomString();

                  st = System.nanoTime();
                  cache.put(fqn, "key", value);
                  d = System.nanoTime() - st;
                  break;
               case GET:
                  st = System.nanoTime();
                  cache.get(fqn, "key");
                  d = System.nanoTime() - st;
                  break;
               case REMOVE:
                  st = System.nanoTime();
                  cache.remove(fqn, "key");
                  d = System.nanoTime() - st;
                  break;
            }
         }
         catch (Exception e) {
            d = 0;
         }
         duration.getAndAdd(d);
      }
   }

   private class Putter extends MyRunnable {
      private Putter(int id, AtomicLong duration) {
         this.id = id;
         this.duration = duration;
         mode = Mode.PUT;
      }
   }

   private class Getter extends MyRunnable {
      private Getter(int id, AtomicLong duration) {
         this.id = id;
         this.duration = duration;
         mode = Mode.GET;
      }
   }

   private class Remover extends MyRunnable {
      private Remover(int id, AtomicLong duration) {
         this.id = id;
         this.duration = duration;
         mode = Mode.REMOVE;
      }
   }

   protected String printDuration(long duration) {
      if (duration > 2000) {
         double dSecs = ((double) duration / (double) 1000);
         return "Duration: " + dSecs + " seconds";
      } else {
         return "Duration: " + duration + " millis";
      }
   }
}
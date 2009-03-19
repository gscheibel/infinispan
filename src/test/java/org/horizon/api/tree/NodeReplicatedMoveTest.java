/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.horizon.api.tree;

import org.horizon.Cache;
import org.horizon.config.Configuration;
import org.horizon.test.MultipleCacheManagersTest;
import org.horizon.test.TestingUtil;
import org.horizon.transaction.DummyTransactionManagerLookup;
import org.horizon.tree.Fqn;
import org.horizon.tree.Node;
import org.horizon.tree.TreeCache;
import org.horizon.tree.TreeCacheImpl;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import org.testng.annotations.Test;

import javax.transaction.TransactionManager;

@Test(groups = "functional", testName = "api.tree.NodeReplicatedMoveTest")
public class NodeReplicatedMoveTest extends MultipleCacheManagersTest {

   static final Fqn A = Fqn.fromString("/a"), B = Fqn.fromString("/b"), C = Fqn.fromString("/c"), D = Fqn.fromString("/d"), E = Fqn.fromString("/e");
   static final Object k = "key", vA = "valueA", vB = "valueB", vC = "valueC", vD = "valueD", vE = "valueE";

   TreeCache<Object, Object> cache1, cache2;
   TransactionManager tm1;

   protected void createCacheManagers() throws Throwable {
      Configuration c = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC);
      c.setInvocationBatchingEnabled(true);
      c.setTransactionManagerLookupClass(DummyTransactionManagerLookup.class.getName());

      createClusteredCaches(2, "replSync", c);

      Cache c1 = cache(0, "replSync");
      Cache c2 = cache(1, "replSync");

      tm1 = TestingUtil.getTransactionManager(c1);

      cache1 = new TreeCacheImpl<Object, Object>(c1);
      cache2 = new TreeCacheImpl<Object, Object>(c2);
   }

   public void testReplicatability() {
      Node<Object, Object> rootNode = cache1.getRoot();

      Node<Object, Object> nodeA = rootNode.addChild(A);
      Node<Object, Object> nodeB = nodeA.addChild(B);

      nodeA.put(k, vA);
      nodeB.put(k, vB);

      assertEquals(vA, cache1.getRoot().getChild(A).get(k));
      assertEquals(vB, cache1.getRoot().getChild(A).getChild(B).get(k));

      assertEquals(vA, cache2.getRoot().getChild(A).get(k));
      assertEquals(vB, cache2.getRoot().getChild(A).getChild(B).get(k));

      // now move...
      cache1.move(nodeB.getFqn(), Fqn.ROOT);

      assertEquals(vA, cache1.getRoot().getChild(A).get(k));
      assertEquals(vB, cache1.getRoot().getChild(B).get(k));

      assertEquals(vA, cache2.getRoot().getChild(A).get(k));
      assertEquals(vB, cache2.getRoot().getChild(B).get(k));
   }

   public void testReplTxCommit() throws Exception {
      Node<Object, Object> rootNode = cache1.getRoot();
      Fqn A_B = Fqn.fromRelativeFqn(A, B);
      Node<Object, Object> nodeA = rootNode.addChild(A);
      Node<Object, Object> nodeB = nodeA.addChild(B);

      nodeA.put(k, vA);
      nodeB.put(k, vB);

      assertEquals(vA, cache1.getRoot().getChild(A).get(k));
      assertEquals(vB, cache1.getRoot().getChild(A).getChild(B).get(k));

      assertEquals(vA, cache2.getRoot().getChild(A).get(k));
      assertEquals(vB, cache2.getRoot().getChild(A).getChild(B).get(k));

      // now move...
      tm1.begin();
      cache1.move(nodeB.getFqn(), Fqn.ROOT);

      assertEquals(vA, cache1.get(A, k));
      assertNull(cache1.get(A_B, k));
      assertEquals(vB, cache1.get(B, k));
      tm1.commit();

      assertEquals(vA, cache1.getRoot().getChild(A).get(k));
      assertEquals(vB, cache1.getRoot().getChild(B).get(k));
      assertEquals(vA, cache2.getRoot().getChild(A).get(k));
      assertEquals(vB, cache2.getRoot().getChild(B).get(k));

   }

   public void testReplTxRollback() throws Exception {
      Node<Object, Object> rootNode = cache1.getRoot();
      Node<Object, Object> nodeA = rootNode.addChild(A);
      Node<Object, Object> nodeB = nodeA.addChild(B);

      nodeA.put(k, vA);
      nodeB.put(k, vB);

      assertEquals(vA, cache1.getRoot().getChild(A).get(k));
      assertEquals(vB, cache1.getRoot().getChild(A).getChild(B).get(k));
      assertEquals(vA, cache2.getRoot().getChild(A).get(k));
      assertEquals(vB, cache2.getRoot().getChild(A).getChild(B).get(k));

      // now move...
      tm1.begin();
      cache1.move(nodeB.getFqn(), Fqn.ROOT);

      assertEquals(vA, cache1.get(A, k));
      assertEquals(vB, cache1.get(B, k));

      tm1.rollback();

      assertEquals(vA, cache1.getRoot().getChild(A).get(k));
      assertEquals(vB, cache1.getRoot().getChild(A).getChild(B).get(k));
      assertEquals(vA, cache2.getRoot().getChild(A).get(k));
      assertEquals(vB, cache2.getRoot().getChild(A).getChild(B).get(k));
   }
}

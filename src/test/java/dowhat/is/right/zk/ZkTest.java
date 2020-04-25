package dowhat.is.right.zk;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import dowhat.is.right.concurrency.ManualResetEvent;
import dowhat.is.right.zk.lock.ZkLockBase;
import dowhat.is.right.zk.lock.ZkReadLock;
import dowhat.is.right.zk.lock.ZkWriteLock;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZkTest {

  private static final String ZK_HOST = "10.39.146.92:2181,10.39.146.92:2182,10.39.146.92:2183";

  @Before
  public void setUp() {
    ZkSessionManager.initializeInstance(ZK_HOST, 10000, Integer.MAX_VALUE);
  }

  @After
  public void tearDown() throws Exception {
    assertNotNull(ZkSessionManager.instance());
    ZkSessionManager.instance().shutdown();
  }


  @Test
  public void testPath() {
    ZkPath zkPath = new ZkPath("/tian/zheng");
    System.out.println(zkPath);
  }

  /**
   * {@link ManualResetEvent}
   */
  @Test
  public void testLock() {
    ZkReadLock zkReadLock = new ZkReadLock("/tian/zheng");
    try {
      boolean b = zkReadLock.tryAcquire();
      if (b) {
        zkReadLock.release();
      }
    } catch (ZkException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testCreateReadLockInRoot() throws Exception {
    createLocksInFolder(ZkReadLock.class, "/");
  }

  @Test
  public void testCreateReadLockInFolder() throws Exception {
    createLocksInFolder(ZkReadLock.class, "/" + (new Random()).nextInt() + "/");
  }

  @Test
  public void testCreateWriteLockInRoot() throws Exception {
    createLocksInFolder(ZkWriteLock.class, "/");
  }

  @Test
  public void testCreateWriteLockInFolder() throws Exception {
    createLocksInFolder(ZkWriteLock.class, "/" + (new Random()).nextInt() + "/");
  }

  private void createLocksInFolder(Class<? extends ZkLockBase> lockClass, String folder)
      throws Exception {
    String rootLockPath = folder + (new Random()).nextInt();
    // Acquire this lock path for first time
    ZkLockBase lock = lockClass.getConstructor(String.class).newInstance(rootLockPath);
    lock.acquire();
    lock.release();
    //Acquire this lock path for second time
    lock = lockClass.getConstructor(String.class).newInstance(rootLockPath);
    lock.acquire();
    lock.release();
  }

  @Test
  public void testCreateContributedKeySet() throws Exception {
    // Need to make sure path of set exists
    ZkPath setPath = new ZkPath("/ClusterMembers");
    setPath.waitSynchronized();
    // Now can add members to set
    ZkContributedKeySet ckSet1 = new ZkContributedKeySet("/ClusterMembers",
        new String[]{"myNodeId1"}, true);
    ZkContributedKeySet ckSet2 = new ZkContributedKeySet("/ClusterMembers",
        new String[]{"myNodeId2"}, true);
    ckSet1.waitSynchronized();
    ckSet2.waitSynchronized();
    assertEquals(ckSet1.getKeySet(), ckSet2.getKeySet());
    assertEquals(2, ckSet1.getKeySet().size());
    assertTrue(ckSet1.getKeySet().contains("myNodeId1"));
    assertTrue(ckSet1.getKeySet().contains("myNodeId2"));
    // Add listener for set updates
    final ManualResetEvent updated = new ManualResetEvent(false);
    Runnable updateCallback = updated::set;
    ckSet1.addUpdateListener(updateCallback, false);
    // Update set contribution
    ckSet1.adjustMyContribution(new String[]{});
    assertTrue(updated.waitOne(5000, TimeUnit.MILLISECONDS));
    assertEquals(1, ckSet1.getKeySet().size());
  }
}

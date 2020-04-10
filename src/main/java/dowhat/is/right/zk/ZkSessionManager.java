package dowhat.is.right.zk;

import dowhat.is.right.concurrency.ManualResetEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

/**
 * {@link ZkSyncPrimitive}
 *
 * <English>
 * A watcher object which will be notified of state changes,
 * <p>
 * may also be notified for node events.
 *
 * <Chinese>
 * zk会话管理。
 * <p>
 * 当事件状态变化或者节点事件时，会通知。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public final class ZkSessionManager implements Watcher {

  private static ZkSessionManager instance;
  //"host1:port1,host2:port2"
  private final String connectString;
  //整个会话是否启动的标识
  private final ManualResetEvent isConnected;
  //线程池
  private final ExecutorService connectExecutor;
  //线程池
  private final ScheduledExecutorService callBackExecutor;
  //互斥锁
  private final Integer retryMutex = -1;
  //超时时间
  private final int sessionTimeout;
  //zk客户端
  volatile ZooKeeper zkClient;
  //是否关闭
  private volatile boolean shutdown;
  //当前复活原语列表
  private Set<ZkSyncPrimitive> currResurrectList;
  //重启后需要重启的原语列表
  private Set<ZkSyncPrimitive> currRestartOnConnectList;
  //最大连接重试次数
  private int maxConnectAttempts;
  /**
   * 创建一个[zk client]线程
   */
  private Callable<ZooKeeper> zkClientCreator = new Callable<ZooKeeper>() {
    @Override
    public ZooKeeper call() throws Exception {
      int attempts = 0;
      int retryDelay = 50;
      while (true) {//return until connected or reach the max connect attempt
        try {
          zkClient = new ZooKeeper(connectString, sessionTimeout, ZkSessionManager.this);
          return zkClient;
        } catch (IOException e) {
          e.printStackTrace();
          attempts++;
          if (maxConnectAttempts != 0 && attempts >= maxConnectAttempts) {
            throw (IOException) e.getCause();
          }
          retryDelay *= 2;//double the connect time
          if (retryDelay > 7500) {
            retryDelay = 7500;
          }
        }
        Thread.sleep(retryDelay);
      }
    }
  };

  private ZkSessionManager(String connectString) {
    this(connectString, 6000, 5);
  }

  private ZkSessionManager(String connectString, int sessionTimeout, int maxConnectAttempts) {
    if (maxConnectAttempts < 1) {
      throw new IllegalArgumentException(
          "max connect attempts must be greater than or equals to 1");
    }
    shutdown = false;
    this.connectString = connectString;
    this.sessionTimeout = sessionTimeout;
    this.maxConnectAttempts = maxConnectAttempts;
    isConnected = new ManualResetEvent(false);
    //一般机器为8核心
    callBackExecutor = Executors.newScheduledThreadPool(8);
    //zk只有一个主线程，回调采用回调线程
    connectExecutor = Executors.newSingleThreadExecutor();
    try {
      connectExecutor.submit(zkClientCreator)
          .get();//we know zookeeper client assigned when past this statement
      isConnected.waitOne();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static ZkSessionManager instance() {
    return instance;
  }

  public static void initializeInstance(String connectString) {
    instance = new ZkSessionManager(connectString);
  }

  public static void initializeInstance(String connectString, int sessionTimeout,
      int maxConnectAttempts) {
    instance = new ZkSessionManager(connectString, sessionTimeout, maxConnectAttempts);
  }

  /**
   * <English>
   * Before destroy, you should shut down.
   * <Chinese>
   * 在杀掉项目前，关闭zk客户端连接和回调线程池。
   *
   * @throws InterruptedException exception
   */
  public void shutdown() throws InterruptedException {
    shutdown = true;
    zkClient.close();
    callBackExecutor.shutdownNow();
  }

  public boolean isShutdown() {
    return shutdown;
  }

  /**
   * <English>
   * retry the primitive runnable.
   *
   * <Chinese>
   * 重试逻辑。
   *
   * @param operation runnable 需要重试的任务
   * @param retries   retry time 重试次数
   */
  void retryPrimitiveOperation(Runnable operation, int retries) {
    if (!shutdown) {
      int delay = 250 + retries * 500;
      if (delay > 7500) {
        delay = 7500;
        callBackExecutor.schedule(operation, delay, TimeUnit.MILLISECONDS);
      }
    }
  }

  /**
   * <English>
   * Restart a primitive when connected.
   *
   * <Chinese>
   * zk客户端连接时，重启原语。
   *
   * @param primitive 原语
   */
  void restartPrimitiveWhenConnected(ZkSyncPrimitive primitive) {
    synchronized (retryMutex) {
      if (currRestartOnConnectList == null) {
        currRestartOnConnectList = Collections.newSetFromMap(new WeakHashMap<>());
      }
      currRestartOnConnectList.add(primitive);
    }
  }

  /**
   * <English>
   * Resurrect primitive when new session.
   *
   * <Chinese>
   * 开启新会话时，复活单个原语。
   *
   * @param primitive 原语
   */
  void resurrectPrimitiveWhenNewSession(ZkSyncPrimitive primitive) {
    synchronized (retryMutex) {
      if (currResurrectList == null) {
        currResurrectList = Collections.newSetFromMap(new WeakHashMap<>());
      }
      currResurrectList.add(primitive);
    }
  }

  @Override
  public void process(WatchedEvent watchedEvent) {
    if (EventType.None == watchedEvent.getType()) {
      KeeperState keeperState = watchedEvent.getState();
      System.out.println("process: " + keeperState);
      switch (keeperState) {
        case SyncConnected:
          onConnected();
          break;
        case Disconnected:
          onDisconnection();
          break;
        case Expired:
          onSessionExpired();
          break;
        default:
          break;
      }
    }
  }

  /**
   * <English>
   * The ZooKeeper client is connected to the ZooKeeper cluster.
   * <p>
   * Actions that modify cluster data may now be performed,
   * <p>
   * Any primitives that were previously suspended after disconnection must be restarted,
   * <p>
   * and any primitives that wished to be resurrected after session expiry,
   * <p>
   * must be asked to resynchronize.
   *
   * <Chinese>
   * ZooKeeper客户端连接到ZooKeeper集群。
   * <p>
   * 可以执行修改集群数据的操作，
   * <p>
   * 必须重新启动以前在断开连接后挂起的所有原语，
   * <p>
   * 并且必须要求希望在会话期满后恢复的所有原语重新同步。
   */
  private void onConnected() {
    synchronized (retryMutex) {
      /*
       * We are going to process the existing lists;
       * We take a copy of the lists and reset them to null to avoid potential re-entrancy problems
       * if the client becomes disconnected again while restarting the waiting primitives thus
       * causing them to try to re-add themselves to these lists.
       */
      Set<ZkSyncPrimitive> resurrectList = currResurrectList;
      Set<ZkSyncPrimitive> restartOnConnectList = currRestartOnConnectList;
      currResurrectList = null;
      currRestartOnConnectList = null;
      //processing resurrection list...
      if (resurrectList != null) {
        System.out
            .println("onConnected processing currResurrectList.size: " + resurrectList.size());
        for (ZkSyncPrimitive primitive : resurrectList) {
          primitive.zkClient = this.zkClient;
          primitive.resynchronize();
        }
      }
      //processing restart on re-connection list
      if (restartOnConnectList != null) {
        for (ZkSyncPrimitive primitive : restartOnConnectList) {
          Runnable retryOnConnect = primitive.retryOnConnect;
          primitive.retryOnConnect = null;// this may be re-assigned by running if disconnect again.
          retryOnConnect.run();
        }
      }
    }
    isConnected.set();
  }

  /**
   * <English>
   * We have been disconnected from ZooKeeper.
   * <p>
   * The client will try to reconnect automatically.
   * <p>
   * However, even after a successful reconnect,
   * <p>
   * we may miss node creation followed by node deletion event.
   * <p>
   * Furthermore, we cannot be *sure* of situation on server while in this state,
   * <p>
   * nor perform actions that require modifying the server state.
   * <p>
   * This may require special handling so we notify our sync objects.
   *
   * <Chinese>
   * 和zk断开连接。
   * <p>
   * 客户端将尝试自动重连。
   * <p>
   * 然而，即使在成功重连，也可能丢掉了节点创建(随之的节点删除也没法完成)。
   * <p>
   * 此外，处于这种状态时，我们不能“确定”服务器上的情况，也不能执行需要修改服务器状态的操作。
   * <p>
   * 这可能需要特殊的处理，所以我们通知同步对象。
   */
  private void onDisconnection() {
    isConnected.reset();
  }

  /**
   * <English>
   * The ZooKeeper session has expired.
   * <p>
   * We need to initiate the creation of a new client session.
   * <p>
   * Primitives that are currently suspended while waiting for re-connection must now be killed,
   * <p>
   * except for the rare case where they can be resurrected when there is a new session.
   *
   * <Chinese>
   * zk会话已经过期。
   * <p>
   * 我们需要开始创建一个新的客户端会话。
   * <p>
   * 在等待重新连接前，当前被挂起的原语必须被立马杀死，除非在有新会话的情况下它们才可能被复活。
   */
  private void onSessionExpired() {
    synchronized (retryMutex) {
      /*
       * <English>
       * Primitives waiting for reconnection before continuing their operations must now die, except for the
       * rare case they wish to be resurrected when there is a new session
       *
       * <Chinese>
       * 在继续操作之前等待重新连接的原语现在必须死亡，除非在有新会话是，它们才有可能被复活。
       */
      if (currRestartOnConnectList != null) {
        for (ZkSyncPrimitive primitive : currRestartOnConnectList) {
          if (primitive.shouldResurrectAfterSessionExpiry()) {
            currResurrectList.add(primitive);
          } else {
            primitive.die(Code.SESSIONEXPIRED);
          }
        }
        // Clear the reconnect list now
        currRestartOnConnectList.clear();
      }
    }
    //会话关闭
    isConnected.reset();
    //尝试创建新的会话
    connectExecutor.submit(zkClientCreator);
  }
}
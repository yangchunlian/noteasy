package dowhat.is.right.zk;

import dowhat.is.right.concurrency.ManualResetEvent;
import java.util.ArrayList;
import java.util.List;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

/**
 * <English>
 * Zookeeper synchronize primitive implements the watcher interface.
 *
 * <Chinese>
 * zk同步原语，实现zk的watcher接口。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public abstract class ZkSyncPrimitive implements Watcher {

  /**
   * <English>
   * Mutex for use synchronizing access to private members.
   *
   * <Chinese>
   * 互斥锁：用于同步访问私有字段（成员）。
   */
  private final Integer mutex;
  /**
   * <English>
   * The zookeeper session handle.
   *
   * <Chinese>
   * zk客户端。
   */
  ZooKeeper zkClient;
  /**
   * <English>
   * Interrupted task in asynchronous operation sequence, which needs to be re-run on connect.
   *
   * <Chinese>
   * 被打断的异步操作任务，连接zk服务后，需要重新启动。
   */
  Runnable retryOnConnect;
  /**
   * <English>
   * The manager of the zookeeper session with operate within.
   *
   * <Chinese>
   * zk会话管理。
   */
  private ZkSessionManager session;
  /**
   * <English>
   * Tasks to be run when the logical state of
   * <p>
   * the primitive changes e.g. a lock acquired, a list get new items.
   *
   * <Chinese>
   * 当原语的逻辑状态改变时（比如得到锁，获得新的消息），需要启动的任务列表。
   */
  private List<Runnable> stateUpdateListeners;
  /**
   * <English>
   * Tasks to be run when the primitive enters into an unsynchronized state
   * <p>
   * i.e. on session expiry.
   *
   * <Chinese>
   * 当原语进入非同步状态（比如会话过期时）时要运行的任务。
   */
  private List<Runnable> dieListeners;
  /**
   * <English>
   * Event that indicates that our state is synchronized
   * <p>
   * and "ready" and client can proceed.
   *
   * <Chinese>
   * 显示事件状态是否准备好让客户端处理。
   */
  private ManualResetEvent isSynchronized;
  /**
   * <English>
   * Exception indicates what killed this synchronization primitive.
   *
   * <Chinese>
   * 杀死同步原语的异常类型。
   */
  private volatile ZkException killedByException;
  /**
   * <English>
   * Number of attempts retrying a task interrupted by some error e.g. timeout
   *
   * <Chinese>
   * 异常（比如超时）造成的重试次数。
   */
  private int reties;

  protected ZkSyncPrimitive(ZkSessionManager session) {
    this.session = session;
    zkClient = this.session.zkClient;
    stateUpdateListeners = null;
    dieListeners = null;
    isSynchronized = new ManualResetEvent(false);
    retryOnConnect = null;
    reties = 0;
    mutex = -1;
  }

  /**
   * <English>
   * Wait until the primitive has reached a synchronized state.
   * <p>
   * If the operation was successful, this is triggered when a derived class calls
   * <Code>onStateChanged()</Code> for the first time.
   * <p>
   * If the operation was unsuccessful, an exception is thrown.
   *
   * <Chinese>
   * 等待原语到达同步状态。
   * <p>
   * 当第一次调用 onStateChanged() 方法时，会触发这个操作。
   * <p>
   * 如果不成功，会抛出异常。
   *
   * @throws ZkException zk exception
   */
  public void waitSynchronized() throws ZkException {
    isSynchronized.waitOne();
    if (getKillerException() != null) {
      throw getKillerException();
    }
  }

  /**
   * <English>
   * Add a listener task to be executed when the object enters the synchronized state,
   * <p>
   * and every time it updates its state thereafter (as marked by derived classes
   * calling<code>onStateUpdated()</code>).
   *
   * <Chinese>
   * 添加一个监听：事件进入同步状态触发。
   * <p>
   * 并在以后每次状态更新时执行。
   *
   * @param handler      The listener task to execute when the state has changed.
   *                     <p>
   *                     A weak reference is taken.
   * @param doStartupRun If the state of the primitive is already synchronized then run the handler
   *                     immediately。
   */
  public void addUpdateListener(Runnable handler, boolean doStartupRun) {
    synchronized (mutex) {
      if (stateUpdateListeners == null) {
        stateUpdateListeners = new ArrayList<>(8);
      }
      //Add to listener set first to avoid reentrancy race.
      stateUpdateListeners.add(handler);
      if (doStartupRun && killedByException == null && isSynchronized.isSignalled()) {
        handler.run();
      }
    }
  }

  /**
   * <English>
   * Remove a task from update listener list.
   *
   * <Chinese>
   * 从更新的任务监听列表中，删除一个。
   *
   * @param handler task
   */
  public void removeUpdateListener(Runnable handler) {
    stateUpdateListeners.remove(handler);
  }

  /**
   * <English>
   * Add a task to die listener list.
   *
   * <Chinese>
   * 往销毁的任务监听列表中，添加一个。
   *
   * @param handler task
   */
  public void addDieListener(Runnable handler) {
    synchronized (mutex) {
      if (dieListeners == null) {
        dieListeners = new ArrayList<>(8);
      }
      // Add to listener set first to avoid reentrancy race.
      dieListeners.add(handler);
      // If we are already synchronized then trigger.
      if (killedByException != null) {
        handler.run();
      }
    }
  }

  /**
   * <English>
   * Return whether the synchronization primitive is still valid / alive.
   *
   * <Chinese>
   * 判断同步原语是否存活。
   *
   * @return whether this primitive is alive and can be used.
   */
  public boolean isAlive() {
    return killedByException != null;
  }

  /**
   * <English>
   * If the primitive has been killed, returns the exception that has killed it.
   *
   * <Chinese>
   * 如果原语已死，返回该异常。
   *
   * @return The exception that killed the primitive.
   */
  public ZkException getKillerException() {
    return killedByException;
  }

  /**
   * <English>
   * Must be called by derived classes when they have successfully updated their state.
   *
   * <Chinese>
   * 当派生类成功更新其状态时，它们必须调用。
   */
  protected void onStateUpdated() {
    synchronized (mutex) {
      killedByException = null;
      // Notify handlers ***before*** signalling synchronized state to allow handlers to perform
      // some pre-processing / prepare the way for a blocked main client thread to proceed
      if (stateUpdateListeners != null) {
        for (Runnable handler : stateUpdateListeners) {
          handler.run();
        }
      }
      // Signal state updated
      isSynchronized.set();
    }
  }

  /**
   * <English>
   * If you have indicated that you wish to resurrect your synchronization primitive after a session
   * expiry or other event that would otherwise kill it
   * <p>
   * - for example by returning <code>true</code> from <code>shouldResurrectOnSessionExpiry()</code>
   * - you need to override this method to perform the re-synchronization steps.
   * <p>
   * You might choose to ressurect/re-synchronize for example in a case where your primitive
   * maintains a listing of nodes in a cluster,
   * <p>
   * and you would rather maintain the last good known record and try to re-synchronize rather than
   * blow up in the case where for some reason a session is expired
   * <p>
   * - for example after ZooKeeper has temporarily gone down or been partitioned.
   * <p>
   * The ZooKeeper documentation warns against libraries that attempt to re-synchronize,
   * <p>
   * but it seems there are some cases where it is valid to do so.
   *
   * <Chinese>
   * 用这个方法在会话过期后，复活你的同步原语，否则就要杀掉。
   * <p>
   * 比如 shouldResurrectOnSessionExpiry() 返回true时，
   * <p>
   * 你需要重写这个方法来执行重新同步的步骤。
   * <p>
   * 你可以在你的集群原语列表中，选择一些进行复活/重新同步，
   * <p>
   * 而且最好保留一个良好的记录，并且重新同步，而不是因为会话过期等这种原因直接崩溃。
   * <p>
   * 比如在zk暂时下线或被分割。
   * <p>
   * zk的文档中，警告要慎重使用重新同步。
   * <p>
   * 但在有些时候，这么做是有作用的。
   */
  protected void resynchronize() {
  }

  /**
   * <English>
   * Override to be notified of death event.
   *
   * <Chinese>
   * 重写方法，用来接收死亡事件的通知。
   *
   * @param killerException zk exception
   */
  protected void onDie(ZkException killerException) {
  }

  /**
   * <English>
   * Override to be notified of connection event.
   *
   * <Chinese>
   * 重写方法， 用来接收连接成功通知。
   */
  protected void onConnected() {
  }

  /**
   * <English>
   * Override to be notified of disconnection event.
   *
   * <Chinese>
   * 重写方法，用来接收失去连接的通知。
   */
  protected void onDisConnected() {
  }

  /**
   * <English>
   * Override to be notified of session expiry event.
   *
   * <Chinese>
   * 重写方法，用来接受会话过期的通知。
   */
  protected void onSessionExpired() {
  }

  /**
   * <English>
   * Override to be notified of node creation event.
   *
   * <Chinese>
   * 重写方法，用来接收节点创建的通知。
   *
   * @param path 节点路径
   */
  protected void onNodeCreated(String path) {

  }

  /**
   * <English>
   * Override to be notified of node deletion event.
   *
   * <Chinese>
   * 重写方法，用来接收节点删除的通知。
   *
   * @param path 节点路劲
   */
  protected void onNodeDeleted(String path) {

  }

  /**
   * <English>
   * Override to be notified of node data changing event.
   *
   * <Chinese>
   * 重写方法，用来接收节点数据变化的事件。
   *
   * @param path 节点路径
   */
  protected void onNodeDataChanged(String path) {

  }

  /**
   * <English>
   * Override to be notified of node children list changed event.
   *
   * <Chinese>
   * 重写方法，用来接收孩子节点列表的变化事件。
   *
   * @param path 孩子节点变化时的父路径。
   */
  protected void onNodeChildrenChanged(String path) {

  }

  /**
   * <English>
   * Override to indicate whether operations should be retried on error.
   *
   * <Chinese>
   * 重写方法，用来表名操作是否需要重试。
   *
   * @return Whether to retry./是否重试
   */
  protected boolean shouldRetryOnError() {
    return false;
  }

  /**
   * <English>
   * Override to indicate whether operations should be retried on timeout error.
   *
   * <Chinese>
   * 重写方法，表明超时后，是否重试
   *
   * @return Whether to retry./是否重试
   */
  protected boolean shouldRetryOnTimeout() {
    return true;
  }

  /**
   * <English>
   * Override to indicate whether to resurrect the primitive and re-synchronize after session
   * expiry.
   * <p>
   * See the comments for <code>resynchronize()</code> for discussions of rare cases where this is
   * desirable. Only do this with extreme caution.
   *
   * <Chinese>
   * 表明恢复会话后，是否恢复原语并重新同步。
   * <p>
   * 请参阅<code>resynchronize()</code>的注释，以了解需要这样做的罕见情况。
   * <p>
   * 一定要非常小心！！！！！！
   *
   * @return Whether to re-synchronize after session expiry
   */
  protected boolean shouldResurrectAfterSessionExpiry() {
    return false;
  }

  /**
   * <English>
   * Return a zkClient.
   *
   * <Chinese>
   * 返回zk客户端。
   *
   * @return zk client
   */
  protected ZooKeeper zkClient() {
    return zkClient;
  }


  /**
   * <English>
   * Permanently kill this synchronization primitive.
   * <p>
   * It cannot be resurrected.
   *
   * <Chinese>
   * 永久终止同步原语。
   * <p>
   * 它不能复活。
   *
   * @param rc The code of the ZooKeeper error that killed this primitive
   */
  protected void die(Code rc) {
    KeeperException keeperException = KeeperException.create(rc);
    die(keeperException);
  }

  /**
   * <English>
   * Permanently kill this synchronization primitive.
   * <p>
   * It cannot be resurrected.
   * <p>
   * This method is typically called by a derived class to pass an exception received from another
   * ZkSyncPrimitive instance it has been using to implement its algorithm.
   *
   * <Chinese>
   * 永久终止同步原语。
   * <p>
   * 不能复活。
   * <p>
   * 这个方法通常由派生类调用，接收另一个同步原语算法产生的异常。
   *
   * @param killerException The killer exception.
   */
  protected void die(KeeperException killerException) {
    die(new ZkException(killerException));
  }

  /**
   * <English>
   * <Chinese>
   *
   * @param killerException kill exception
   */
  protected void die(ZkException killerException) {
    synchronized (mutex) {
      // Record that we have been killed off by the exception passed by a derived class.
      // This might have been generated by a contained ZkSyncPrimitive object we were
      // using in the course of an algorithm
      this.killedByException = killerException;
      // Call into derived event handler
      onDie(killerException);
      // Notify listeners ***before*** signalling state update to allow pre-processing of error
      if (dieListeners != null) {
        for (Runnable handler : dieListeners) {
          handler.run();
        }
      }
      // Death is a synchronized state!
      isSynchronized.set();
    }
  }

  /**
   * <English>
   * Prepares the next step in an asynchronous execution,
   * <p>
   * based upon the return code from the previous step.
   *
   * <Chinese>
   * 根据上一步返回的code，
   * <p>
   * 执行下一步的异步操作。
   * <p>
   * 如果上一步正常，直接pass。
   * <p>
   * 否则根据状态，对应的进行操作。
   *
   * @param rc         The Zookeeper return code from the previous step
   * @param acceptable The acceptable list of return codes from the previous step
   * @param operation  The operation from the previous step（provided so it might be retried Whether
   *                   the next step should be started）
   * @return Whether the next step should be started./下个操作是否允许。
   */
  protected boolean passOrTryRepeat(int rc, Code[] acceptable, Runnable operation) {
    Code opResult = Code.get(rc);
    for (Code code : acceptable) {
      if (opResult == code) {
        reties = 0;
        return true;
      }
    }
    //The operation result was not acceptable. We will either retry or die...
    switch (opResult) {
      case CONNECTIONLOSS:
        retryOnConnect = operation;
        session.restartPrimitiveWhenConnected(this);
        break;
      case SESSIONMOVED:// We assume that this is caused by request flowing over "old" connection, will be resolve with time.
      case OPERATIONTIMEOUT:
        if (shouldRetryOnTimeout()) {
          retryAfterDelay(operation, reties++);
        }
        break;
      case SESSIONEXPIRED:
        onSessionExpiry(opResult);
        break;
      default:
        if (shouldRetryOnError()) {
          retryAfterDelay(operation, reties++);
        } else {
          die(opResult);
        }
        break;
    }
    return false;
  }

  /**
   * <English>
   * Trigger operate when receive watch event from zk server.
   *
   * <Chinese>
   * 接收到服务端的事件事，触发操作。
   *
   * @param watchedEvent watch event
   */
  @Override
  public void process(WatchedEvent watchedEvent) {
    String eventPath = watchedEvent.getPath();
    EventType eventType = watchedEvent.getType();
    KeeperState keeperState = watchedEvent.getState();
    if (KeeperState.Expired == keeperState) {
      onSessionExpiry(Code.SESSIONEXPIRED);
      return;
    }
    switch (eventType) {
      case NodeCreated:
        onNodeCreated(eventPath);
        break;
      case NodeDeleted:
        onNodeDeleted(eventPath);
        break;
      case NodeDataChanged:
        onNodeDataChanged(eventPath);
        break;
      case NodeChildrenChanged:
        onNodeChildrenChanged(eventPath);
        break;
      default:
        die(Code.SYSTEMERROR);
        break;
    }
  }

  /**
   * <English>
   * Trigger the operate when session expiry.
   *
   * <Chinese>
   * 当会话过期时触发。
   *
   * @param dieReason die reason
   */
  private void onSessionExpiry(Code dieReason) {
    if (shouldResurrectAfterSessionExpiry()) {
      session.resurrectPrimitiveWhenNewSession(this);
    } else {
      die(dieReason);
    }
  }

  /**
   * <English>
   * Delay a time to retry.
   * <Chinese>
   * 延迟一定时间后重试。
   *
   * @param operation task
   * @param retries   retry times
   */
  private void retryAfterDelay(Runnable operation, int retries) {
    session.retryPrimitiveOperation(operation, retries);
  }
}

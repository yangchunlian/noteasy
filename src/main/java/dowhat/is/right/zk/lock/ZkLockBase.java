package dowhat.is.right.zk.lock;

import dowhat.is.right.zk.ZkException;
import dowhat.is.right.zk.ZkPath;
import dowhat.is.right.zk.ZkSessionManager;
import dowhat.is.right.zk.ZkSyncPrimitive;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.zookeeper.AsyncCallback.ChildrenCallback;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.common.PathUtils;
import org.apache.zookeeper.data.Stat;

/**
 * <English>
 * Base class for single path read and write locks.
 *
 * <Chinese>
 * 用于单路径读写锁的基类。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public abstract class ZkLockBase extends ZkSyncPrimitive implements
    ISinglePathLock {

  //互斥锁
  private final Integer mutex;
  //锁路径
  private String lockPath;
  //上下文
  private Object context;
  //zk路径
  private ZkPath zkPath;
  //当前nodeId
  private String thisNodeId;
  //当前阻塞的锁nodeId
  private String blockingNodeId;
  //监听器
  private ILockListener listener;
  //是否只是尝试获取锁
  private boolean tryAcquireOnly;
  //锁状态
  private volatile LockState lockState;
  private VoidCallback releaseLockHandler =
      (rc, path, ctx) -> passOrTryRepeat(rc, new Code[]{Code.OK, Code.NONODE}, (Runnable) ctx);
  /**
   * Only delete the node.
   */
  private Runnable releaseLock = new Runnable() {
    @Override
    public void run() {
      zkClient().delete(zkPath.getTargetPath() + "/" + thisNodeId, -1, releaseLockHandler, this);
    }
  };
  private Runnable onLockPathError = new Runnable() {
    @Override
    public void run() {
      // Set the lock state
      safeLockState(LockState.ERROR);
      // Bubble the killer exception and die!
      die(zkPath.getKillerException());
    }
  };
  /**
   * Call back for <code>getQueuedLocks</code>
   */
  private ChildrenCallback queuedLocksHandler = new ChildrenCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children) {
      // Upon successful enumeration of lock nodes, see if any are blocking this...
      // 查看孩子节点，是否有阻塞父节点的情况
      if (passOrTryRepeat(rc, new Code[]{Code.OK}, (Runnable) ctx)) {
        // Create sorted list of nodes.
        SortedSet<ZkLockNode> nodeQueue = new TreeSet<>();
        for (String lockId : children) {
          ZkLockNode zkLockNode = ZkLockNode.lockNodeFromId(lockId, ZkLockBase.this.thisNodeId);
          nodeQueue.add(zkLockNode);
        }
        // Check who is blocking this.
        ZkLockNode self = null;
        ZkLockNode prevNode = null;
        ZkLockNode prevWriteNode = null;
        for (ZkLockNode node : nodeQueue) {
          if (node.self) {
            self = node;
            break;// Break, not continue. Indicate no children.
          }
          prevNode = node;
          if (prevNode.lockType == LockType.WRITE) {
            prevWriteNode = prevNode;
          }
        }
        // Blocking node
        ZkLockNode blockingNode;
        if (self != null && LockType.READ == self.lockType) {
          blockingNode = prevWriteNode;
        } else {
          blockingNode = prevNode;
        }
        // Are we blocked?
        if (blockingNode != null) {
          blockingNodeId = blockingNode.name;
          // Should we give up, or wait?
          if (tryAcquireOnly) {// We abandon attempt to acquire
            safeLockState(LockState.ABANDONED);
          } else {// Wait for blocking node.
            watchBlockingNode.run();
          }
        } else {
          // Children do not have lock, we are acquired!
          safeLockState(LockState.ACQUIRED);
        }
      }
    }
  };
  /**
   * Get the children locks.
   */
  private Runnable getQueuedLocks = new Runnable() {
    @Override
    public void run() {
      zkClient().getChildren(zkPath.getTargetPath(), null, queuedLocksHandler, this);
    }
  };
  private StatCallback blockingNodeHandler = new StatCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
      if (rc == Code.NONODE.intValue()) {
        getQueuedLocks.run();
      } else {
        passOrTryRepeat(rc, new Code[]{Code.OK}, (Runnable) ctx);
      }
    }
  };
  private Runnable watchBlockingNode = new Runnable() {
    @Override
    public void run() {
      String path = zkPath.getTargetPath() + "/" + blockingNodeId;
      zkClient().exists(path, ZkLockBase.this, blockingNodeHandler, this);
    }
  };
  /**
   * Create a call back for <code>createLockNode</code>
   * <p>
   * if ok, get the nodeId
   * <p>
   * else, ...
   */
  private StringCallback createLockNodeHandler = new StringCallback() {
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
      if (Code.OK.intValue() == rc) {
        thisNodeId = ZkLockNode.getLockNodeIdFromName(name);
      }
      if (passOrTryRepeat(rc, new Code[]{Code.OK}, (Runnable) ctx)) {
        getQueuedLocks.run();
      }
    }
  };
  /**
   * Create lock node in the target path.
   */
  private Runnable createLockNode = new Runnable() {
    @Override
    public void run() {
      String path = zkPath.getTargetPath() + "/" + getType() + "-";
      zkClient().create(
          path,
          new byte[0],
          ZooDefs.Ids.OPEN_ACL_UNSAFE,
          CreateMode.EPHEMERAL_SEQUENTIAL,
          createLockNodeHandler,
          this);
    }
  };
  private Runnable reportStateUpdatedToListener = new Runnable() {
    @Override
    public void run() {
      if (tryAcquireOnly && lockState != LockState.ACQUIRED) {
        // We know that an error has not occurred, because that is passed to handler below. So report attempt
        // to acquire locked failed because was already held.
        ITryLockListener listener = (ITryLockListener) ZkLockBase.this.listener;
        listener.onTryAcquireLockFailed(ZkLockBase.this, context);
      } else {
        listener.onLockAcquired(ZkLockBase.this, context);
      }
    }
  };
  private Runnable reportDieToListener = new Runnable() {
    @Override
    public void run() {
      listener.onLockError(getKillerException(), ZkLockBase.this, context);
    }
  };

  public ZkLockBase(String lockPath) {
    super(ZkSessionManager.instance());
    PathUtils.validatePath(lockPath);
    lockState = LockState.IDLE;
    this.lockPath = lockPath;
    mutex = -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acquire() throws ZkException, InterruptedException {
    setLockState(LockState.WAITING);
    createRootPath(lockPath);
    waitSynchronized();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acquire(ILockListener listener, Object context)
      throws ZkException, InterruptedException {
    setLockState(LockState.WAITING);
    this.listener = listener;
    this.context = context;
    addUpdateListener(reportStateUpdatedToListener, false);
    addDieListener(reportDieToListener);
    createRootPath(lockPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean tryAcquire() throws ZkException, InterruptedException {
    setLockState(LockState.WAITING);//Only the idle state can set waiting.
    tryAcquireOnly = true;
    createRootPath(lockPath);
    waitSynchronized();
    return lockState == LockState.ACQUIRED;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void tryAcquire(ITryLockListener listener, Object context)
      throws ZkException, InterruptedException {
    setLockState(LockState.WAITING);
    this.listener = listener;
    this.context = context;
    tryAcquireOnly = true;
    addUpdateListener(reportStateUpdatedToListener, false);
    addDieListener(reportDieToListener);
    createRootPath(lockPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    safeLockState(LockState.RELEASED);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LockState getState() {
    return lockState;
  }

  /**
   * What path does this instance lock?
   *
   * @return The path that has/will be locked by this lock instance
   */
  @Override
  public String getLockPath() {
    return lockPath;
  }

  @Override
  public int compareTo(ISinglePathLock other) {
    int result = getLockPath().compareTo(other.getLockPath());
    return result == 0 ? (getType() == other.getType() ? 1 : 0) : result;
  }

  /**
   * <English>
   * create root path
   *
   * <Chinese>
   * 创建根路径（只创建路径）
   *
   * @param path 路径
   */
  private void createRootPath(String path) {
    zkPath = new ZkPath(path, CreateMode.PERSISTENT);
    // TODO for now only persistent ZK nodes can have children. fix this.
    zkPath.addUpdateListener(createLockNode, true);
    zkPath.addDieListener(onLockPathError);
  }

  @Override
  protected void onDie(ZkException killerException) {
    // We just set the lock state. The killer exception has already been set by base class
    safeLockState(LockState.ERROR);
  }

  @Override
  protected void onNodeDeleted(String path) {
    getQueuedLocks.run();
  }

  /**
   * Set the lock state when we know an exception can't be thrown
   *
   * @param newState The new lock state
   */
  private void safeLockState(LockState newState) {
    try {
      setLockState(newState);
    } catch (ZkException e) {
      e.printStackTrace();
      assert false : "Unknown condition";
    }
  }

  /**
   * <English>
   * Set the lock state.
   *
   * <Chinese>
   * 设置锁的状态。
   *
   * @param newState The new lock state
   * @throws ZkException zk exception
   */
  private void setLockState(LockState newState) throws ZkException {
    synchronized (mutex) {
      switch (newState) {
        case IDLE:
          assert false : "Unknown condition";
        case WAITING:
          /*
           * <English>
           * We only set this state from the public interface methods.
           * This means we can directly throw an exception back at the caller!
           * <Chinese>
           * 只在公共接口设置这个状态，这样可在调用时，直接抛出异常
           */
          switch (lockState) {
            case IDLE:
              // Caller is starting operation
              lockState = newState;
              return;
            case WAITING:
              throw new ZkException(ZkException.Error.LOCK_ALREADY_WAITING);
            case ABANDONED:
              throw new ZkException(ZkException.Error.LOCK_ALREADY_ABANDONED);
            case ACQUIRED:
              throw new ZkException(ZkException.Error.LOCK_ALREADY_ACQUIRED);
            case RELEASED:
              throw new ZkException(ZkException.Error.LOCK_ALREADY_RELEASED);
            default:
              assert false : "Unknown condition";
          }
          break;
        case ABANDONED:
          /*
           * <English>
           * We tried to acquire a lock,
           * but it was already held and we are abandoning our attempt to acquire.
           * <Chinese>
           * 在尝试获取锁时，如果锁已经被持有，我们放弃重试。
           */
          switch (lockState) {
            case WAITING:
              // Attempt to acquire lock without blocking has failed
              lockState = newState;
              // Release our lock node immediately
              releaseLock.run();
              // Notify listeners about result
              if (listener != null) {
                ITryLockListener listener = (ITryLockListener) this.listener;
                listener.onTryAcquireLockFailed(this, context);
              }
              // Notify waiting callers about result
              onStateUpdated();
              return;
            case RELEASED:
              // Logically the lock has already been released. No node was created, so no need to releaseLock.run()
              return;
            default:
              assert false : "Unknown condition";
          }
          break;
        case ACQUIRED:
          /*
           * We have successfully acquired the lock.
           */
          switch (lockState) {
            case WAITING:
              // Attempt to acquire lock has succeeded
              lockState = newState;
              // Notify caller
              onStateUpdated();
              return;
            case RELEASED:
            case ERROR:
              // The lock has already been logically released or an error occurred. We initiate node release, and return
              releaseLock.run();
              return;
            default:
              assert false : "Unknown condition";
          }
          break;
        case RELEASED:
          /*
           * We are releasing a lock.
           * This can be done before a lock has been acquired
           * if an operation is in progress.
           */
          switch (lockState) {
            case IDLE:
              /*
               * Change to the released state to prevent this lock
               * being used again
               */
              lockState = newState;
              return;
            case RELEASED:
            case ABANDONED:
              // We consider that release() has been called vacuously
              return;
            case WAITING:
              // release() method called while waiting to acquire lock (or during the process of trying to acquire a lock).
              // This causes an error!
              die(new ZkException(
                  ZkException.Error.LOCK_RELEASED_WHILE_WAITING)); // die callback will set state
              return;
            case ACQUIRED:
              // We are simply releasing the lock while holding it. This is fine!
              lockState = newState;
              // Initiate the release procedure immediately
              releaseLock.run();
              return;
            default:
              assert false : "Unknown condition";
          }
          break;
        case ERROR:
          switch (lockState) {
            case RELEASED:
              // Error is vacuous now. Lock has already been released (or else, break in session will cause ephemeral node to disappear etc)
              return;
            default:
              // ZkSyncPrimitive infrastructure is handling passing exception notification to caller, so just set state
              lockState = newState;
              return;
          }
      }
      assert false : "Unknown condition";
    }
  }
}

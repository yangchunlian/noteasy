package dowhat.is.right.zk.lock;

import dowhat.is.right.networking.utility.NetworkAlgorithms;
import dowhat.is.right.zk.ZkException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

/**
 * todo
 *
 * @author 杨春炼
 * @since 2020-04-05
 */
public class ZkMultiPathLock implements IMultiPathLock {

  private final int MIN_RETRY_DELAY = 125;
  private final int MAX_RETRY_DELAY = 4000;
  private final int MAX_ACQUIRE_ATTEMPTS = 10;
  private ArrayList<ISinglePathLock> locks;
  private ISinglePathLock[] sortedLocks;
  private volatile LockState lockState;
  private final Integer mutex;

  public ZkMultiPathLock() {
    lockState = LockState.IDLE;
    locks = new ArrayList<>(32);
    mutex = -1;
  }

  /**
   * Add a write lock requirement to the multi-lock
   *
   * @param lockPath The read lock path to add
   */
  public void addReadLock(String lockPath) {
    locks.add(new ZkReadLock(lockPath));
  }

  /**
   * Add a write lock requirement to the multi-lock
   *
   * @param lockPath The write lock path to add
   */
  public void addWriteLock(String lockPath) {
    locks.add(new ZkWriteLock(lockPath));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acquire() throws ZkException, InterruptedException {
    setLockState(LockState.WAITING);
    if (locks.size() == 0)
    // We succeed vacuously for zero paths.
    {
      return;
    }
    prepareSortedLockArray();
    if (sortedLocks.length == 1) {
      // With a single lock path, we can just wait.
      sortedLocks[0].acquire();
    } else {
      // With multiple paths, we must try to acquire sequentially, then back off completely
      // if we fail to acquire any
      int attempts = 0;
      while (true) {
        // Try to acquire paths
        if (doTryAcquire()) {
          // got them
          setLockState(LockState.ACQUIRED);
          return;
        }
        // Not this time, and we give up and throw after max attempts
        attempts++;
        if (attempts > MAX_ACQUIRE_ATTEMPTS) {
          throw new ZkException(ZkException.Error.MAX_ATTEMPTS_EXCEEDED);
        }
        // Otherwise back off a little then try again
        Thread.sleep(
            NetworkAlgorithms.getBinaryBackoffDelay(attempts, MIN_RETRY_DELAY, MAX_RETRY_DELAY));
        // To avoid race conditions with the asynchronous release system used by ZkLockBase, we simply re-create the
        // sorted locks array before trying again
        ISinglePathLock[] newSortedLocks = new ISinglePathLock[sortedLocks.length];
        for (int l = 0; l < sortedLocks.length; l++) {
          if (sortedLocks[l] instanceof ZkReadLock) {
            newSortedLocks[l] = new ZkReadLock(sortedLocks[l].getLockPath());
          } else if (sortedLocks[l] instanceof ZkWriteLock) {
            newSortedLocks[l] = new ZkWriteLock(sortedLocks[l].getLockPath());
          } else {
            assert false : "Unrecognized lock type";
          }
        }
        sortedLocks = newSortedLocks;
      }
    }
  }

  private String describe() {
    StringBuilder description = new StringBuilder();
    for (ISinglePathLock sortedLock : sortedLocks) {
      if (sortedLock instanceof ZkReadLock) {
        description.append("ZkReadLock(").append(sortedLock.getLockPath()).append(") ");
      } else if (sortedLock instanceof ZkWriteLock) {
        description.append("ZkWriteLock(").append(sortedLock.getLockPath()).append(") ");
      }
    }
    return description.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void acquire(ILockListener listener, Object context) {
    throw new NotImplementedException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean tryAcquire() throws ZkException, InterruptedException {
    setLockState(LockState.WAITING);
    prepareSortedLockArray();
    if (doTryAcquire()) {
      setLockState(LockState.ACQUIRED);
      return true;
    }
    setLockState(LockState.ABANDONED);
    return false;
  }

  /**
   * When locks are being acquired sequentially, this will produce the earliest possible and least
   * expensive back off when there is contention by similar operations (since multi-locks will try
   * to acquire the same paths first and conflict as early as possible)
   */
  protected void prepareSortedLockArray() {
    sortedLocks = locks.toArray(new ISinglePathLock[0]);
    Arrays.sort(sortedLocks);
  }

  protected boolean doTryAcquire() throws ZkException, InterruptedException {
    // loop until we acquire all locks
    try {
      boolean tryFailed = false;
      // try and get them all
      for (ILock lock : sortedLocks) {
        if (!lock.tryAcquire()) {
          tryFailed = true;
          break;
        }
      }
      // done if successful
      if (!tryFailed) {
        return true;
      }
      // otherwise roll back any locks we acquired
      release();
      return false;
    } catch (ZkException ex) {
      // roll back and re-throw
      setLockState(LockState.ERROR);
      release();
      throw ex;
    } catch (InterruptedException ex) {
      // roll back and re-throw
      setLockState(LockState.ERROR);
      release();
      Thread.currentThread().interrupt();
      throw ex;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void tryAcquire(ITryLockListener listener, Object context) {
    throw new NotImplementedException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    synchronized (mutex) {
      if (sortedLocks != null) {
        for (ILock lock : sortedLocks) {
          if (lock.getState() == LockState.ACQUIRED) {
            lock.release();
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LockState getState() {
    return lockState;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LockType getType() {
    return LockType.MULTI;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getReadLockPaths() {
    return getLockPathsByType(LockType.READ);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getWriteLockPaths() {
    return getLockPathsByType(LockType.WRITE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LockType contains(String lockPath) {
    ISinglePathLock[] currLocks =
        lockState == LockState.IDLE ? locks.toArray(new ISinglePathLock[0]) : sortedLocks;
    for (ISinglePathLock lock : currLocks) {
      if (lock.getLockPath().equals(lockPath)) {
        return lock.getType();
      }
    }
    return LockType.NONE;
  }

  private String[] getLockPathsByType(ILock.LockType type) {
    ArrayList<String> lockPaths = new ArrayList<>(32);
    List<ISinglePathLock> lockList = locks;
    for (ISinglePathLock lock : lockList) {
      if (lock.getType() == type) {
        lockPaths.add(lock.getLockPath());
      }
    }
    return lockPaths.toArray(new String[]{});
  }

  /**
   * Set the lock state
   *
   * @param newState The new lock state
   * @throws ZkException
   */
  private void setLockState(LockState newState) throws ZkException {
    synchronized (mutex) {
      switch (newState) {
        case IDLE:
          assert false : "Unknown condition";
        case WAITING:
          /*
           * We only set this state from the public interface methods. This means we can directly throw an
           * exception back at the caller.
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
           * We tried to acquire a lock, but it was already held and we are abandoning our attempt to acquire.
           */
          switch (lockState) {
            case WAITING:
              // Attempt to acquire lock without blocking has failed
              lockState = newState;
              return;
            case RELEASED:
            case ERROR:
              // The lock nodes have already been released. No need to releaseAll();
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
              return;
            default:
              assert false : "Unknown condition";
          }
          break;
        case RELEASED:
          /*
           * We are releasing a lock. This can be done before a lock has been acquired if an operation is in progress.
           */
          switch (lockState) {
            case IDLE:
              // Change to the released state to prevent this lock being used again
              lockState = newState;
              return;
            case WAITING:
              throw new ZkException(ZkException.Error.LOCK_RELEASED_WHILE_WAITING);
            case ACQUIRED:
              // We are simply releasing the lock while holding it. Everything fine.
              lockState = newState;
            case RELEASED:
            case ABANDONED:
              // We consider that release() has been called vacuously
              return;
            default:
              assert false : "Unknown condition";
          }
          break;
        case ERROR:
          switch (lockState) {
            case RELEASED:
            case ERROR:
              // Error is vacuous now. Locks have already been released.
              return;
            default:
              // An error has occurred.
              lockState = newState;
              return;
          }
      }
      assert false : "Unknown condition";
    }
  }
}

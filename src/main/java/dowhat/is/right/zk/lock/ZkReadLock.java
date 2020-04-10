package dowhat.is.right.zk.lock;

/**
 * <English>
 * Zookeeper read lock.
 *
 * <Chinese>
 * zk读锁。
 *
 * @author 杨春炼
 * @since 2020-04-05
 */
public class ZkReadLock extends ZkLockBase {


  public ZkReadLock(String lockPath) {
    super(lockPath);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LockType getType() {
    return LockType.READ;
  }
}

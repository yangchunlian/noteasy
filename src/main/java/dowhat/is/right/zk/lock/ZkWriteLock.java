package dowhat.is.right.zk.lock;

/**
 * <English>
 * zookeeper write lock
 *
 * <Chinese>
 * zk写锁
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public class ZkWriteLock extends ZkLockBase {

  public ZkWriteLock(String lockPath) {
    super(lockPath);
  }

  /**
   * {@inheritDoc}
   *
   * @return lock type
   */
  @Override
  public LockType getType() {
    return LockType.WRITE;
  }
}

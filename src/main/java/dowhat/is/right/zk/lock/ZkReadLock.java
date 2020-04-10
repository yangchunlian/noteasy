package dowhat.is.right.zk.lock;

/**
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

package dowhat.is.right.zk.lock;

/**
 * @author 杨春炼
 * @since 2020-04-03
 */
public interface ITryLockListener extends ILockListener {


  /**
   * <English>
   * The attempt to acquire a lock failed because it was already held.
   * <Chinese>
   * 获取锁失败，因锁已被持有
   *
   * @param lock    The lock instance that failed to acquire./获取锁失败
   * @param context The context object passed to the lock`s constructor./锁的上下文
   */
  void onTryAcquireLockFailed(ILock lock, Object context);
}

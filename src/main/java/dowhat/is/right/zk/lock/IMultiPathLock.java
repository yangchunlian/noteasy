package dowhat.is.right.zk.lock;

/**
 * @author 杨春炼
 * @since 2020-04-03
 */
public interface IMultiPathLock extends ILock {

  /**
   * <English>
   * Get all the read lock paths the lock will acquire.
   *
   * <Chinese>
   * 获取读锁的所有路径。
   *
   * @return The read lock paths./锁的所有路径
   */
  String[] getReadLockPaths();

  /**
   * <English>
   * Get all the write lock paths the lock will acquire.
   *
   * <Chinese>
   * 获取写锁的所有路径。
   *
   * @return The write lock paths
   */
  String[] getWriteLockPaths();

  /**
   * <English>
   * Discover whether the multilock is or will lock a specific path.
   *
   * <Chinese>
   * 是否包含指定路径上锁的类型。
   *
   * @param lockPath The lock path to query./查询路径
   * @return The type of the lock on that path. LockType.None if no lock for path./查看路径上是否加锁
   */
  LockType contains(String lockPath);

}

package dowhat.is.right.zk.lock;

/**
 * @author 杨春炼
 * @since 2020-04-03
 */
public interface ISinglePathLock extends ILock, Comparable<ISinglePathLock> {

  /**
   * <English>
   * Get the path that a lock is operating against
   * <Chinese>
   * 获取单个锁操作的路径
   *
   * @return The path the lock is attempting to lock./准备加锁的路径
   */
  String getLockPath();
}

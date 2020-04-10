package dowhat.is.right.zk.lock;

import dowhat.is.right.zk.ZkException;

/**
 * <English>
 * This class has two methods which are call back methods when a lock is acquired and when the lock
 * is released.
 * <Chinese>
 * 这个类有两个回调方法，接受获取到锁或者锁释放的状态。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public interface ILockListener {


  /**
   * <English>
   * A lock has been successfully acquired.
   * <Chinese>
   * 获得锁的时候：成功
   *
   * @param lock    The lock that has been acquired./当下的锁
   * @param context The context object passed to ths lock's constructor./锁的上下文
   */
  void onLockAcquired(ILock lock, Object context);

  /**
   * <English>
   * An error has occurred while waiting for the lock.
   * <Chinese>
   * 获取所得时候：失败
   *
   * @param err     The error that has occurred./错误信息
   * @param lock    The lock that has experienced the error./当下的锁
   * @param context The context object passed to the lock`s constructor./锁的上下文
   */
  void onLockError(ZkException err, ILock lock, Object context);
}

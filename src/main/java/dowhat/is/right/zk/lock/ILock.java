package dowhat.is.right.zk.lock;

import dowhat.is.right.zk.ZkException;

/**
 * <English>
 * the lock operate methods
 * <Chinese>
 * 定义锁的操作
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public interface ILock {

  /**
   * <English>
   * Try to acquire the lock. Block until the lock is acquired, or an error occurs.
   * <p>
   * An exception thrown if an error occurs.
   *
   * <Chinese>
   * 尝试获取锁，阻塞直到获取到锁或者抛出异常。
   *
   * @throws ZkException          zk exception
   * @throws InterruptedException zk interrupt exception
   */
  void acquire() throws ZkException, InterruptedException;

  /**
   * <English>
   * Asynchronously try to acquire the lock. The listener object receives the result.
   * <Chinese>
   * 异步获取锁，监听器的上下文接收结果
   *
   * @param listener 监听器
   * @param context  上下文
   * @throws ZkException          zk exception
   * @throws InterruptedException zk interrupt exception
   */
  void acquire(ILockListener listener, Object context) throws ZkException, InterruptedException;

  /**
   * <English>
   * Try to acquire the lock in a synchronous manner, but return without waiting if the lock is
   * already held by another instance. Note this method is not asynchronous because blocking network
   * IO can be performed to determine whether a lock is already held by another instance.
   * <Chinese>
   * 尝试同步获取锁，但是如果锁已被持有，不会继续等待。
   * <p>
   * 注意这不是异步方法，因为同步网络io下，能够判断锁是否已经被持有。
   *
   * @return Whether the lock has been acquired.//判断锁是否被持有
   * @throws ZkException          zk exception
   * @throws InterruptedException zk interrupted exception
   */
  boolean tryAcquire() throws ZkException, InterruptedException;

  /**
   * <English>
   * Try to acquire the lock in an asynchronous manner. If the lock is already held, the operation
   * gives up without waiting, This method does not wait for any blocking IO and the result is
   * returned through the provided listener interface.
   * <Chinese>
   * 争取异步获取锁。如果锁已被持有就不再等待。
   * <p>
   * 这个方法不等待网络io情况。结果通过监听器返回。
   *
   * @param listener 监听器 The listener object to be notified of the result.
   * @param context  上下文
   */
  void tryAcquire(ITryLockListener listener, Object context)
      throws ZkException, InterruptedException;

  /**
   * Release the lock.
   */
  void release();

  /**
   * 锁的状态
   */
  enum LockState {
    IDLE("空闲"),
    WAITING("等待"),
    ABANDONED("已遗弃"),
    ACQUIRED("已获得"),
    RELEASED("已释放"),
    ERROR("错误"),
    ;
    private String des;

    LockState(String des) {
      this.des = des;
    }
  }

  /**
   * Determines the state of the lock.
   *
   * @return The lock state.
   */
  LockState getState();

  /**
   * 锁的类型
   */
  enum LockType {
    READ("读"),
    WRITE("写"),
    MULTI("批量"),
    NONE("nil"),
    ;

    private String des;

    LockType(String des) {
      this.des = des;
    }
  }

  /**
   * Determines the type of the lock.
   *
   * @return The lock type.
   */
  LockType getType();
}

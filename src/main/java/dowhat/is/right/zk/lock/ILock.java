package dowhat.is.right.zk.lock;

import dowhat.is.right.zk.ZkException;
import lombok.Getter;

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
   * Try to acquire the lock in a synchronous manner,
   * <p>
   * but return without waiting if the lock is already held by another instance.
   * <p>
   * Note this method is not asynchronous because blocking network IO can be performed to determine
   * whether a lock is already held by another instance.
   *
   * <Chinese>
   * 同步获取锁，
   * <p>
   * 如果锁已被其他zk客户端持有，不会继续等待。
   * <p>
   * 注意不是异步方法，因为同步网络io下，能够判断锁是否已经被持有。
   *
   * @return Whether the lock has been acquired.//判断锁是否被持有
   * @throws ZkException          zk exception
   * @throws InterruptedException zk interrupted exception
   */
  boolean tryAcquire() throws ZkException, InterruptedException;

  /**
   * <English>
   * Try to acquire the lock in an asynchronous manner.
   * <p>
   * If the lock is already held, the operation gives up without waiting,
   * <p>
   * This method does not wait for any blocking IO and the result is returned through the provided
   * listener interface.
   *
   * <Chinese>
   * 异步获取锁。
   * <p>
   * 如果锁已被持有直接放弃。
   * <p>
   * 此方法不阻塞，通过监听器直接返回结果。
   *
   * @param listener The listener object to be notified of the result./监听器
   * @param context  上下文
   */
  void tryAcquire(ITryLockListener listener, Object context)
      throws ZkException, InterruptedException;

  /**
   * <English>
   * Release the lock.
   *
   * <Chinese>
   * 释放锁。
   */
  void release();

  /**
   * <English>
   * Determines the state of the lock.
   *
   * <Chinese>
   * 获取锁的状态。
   *
   * @return The lock state./锁的类型
   */
  LockState getState();

  /**
   * <English>
   * Determines the type of the lock.
   *
   * <Chinese>
   * 获取所得类型。
   *
   * @return The lock type./锁的类型
   */
  LockType getType();

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
    @Getter
    private String des;

    LockState(String des) {
      this.des = des;
    }
  }

  /**
   * 锁的类型
   */
  enum LockType {
    READ("读锁"),
    WRITE("写锁"),
    MULTI("批量"),
    NONE("无锁"),
    ;
    @Getter
    private String des;

    LockType(String des) {
      this.des = des;
    }
  }
}

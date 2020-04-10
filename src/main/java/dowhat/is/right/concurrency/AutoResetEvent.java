package dowhat.is.right.concurrency;


import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * <English>
 * A simple event which can be either signalled or non-signalled.
 * <p>
 * Waiting for a signalled event does not block. Waiting for a non-signalled event blocks until the
 * event becomes signalled or the thread is interrupted. When a thread waits for the event and then
 * returns, it automatically sets the event to a non-signalled state. Therefore, if the event starts
 * in a non-signalled state, the number of waiting threads it has allowed to pass is equal to the
 * number of times it has been signalled via set().
 * <Chinese>
 * 一个可以发出信号或不发出信号的简单事件。
 * <p>
 * 等待一个信号事件不会阻塞。
 * <p>
 * 等待一个无信号的事件会阻塞，直到事件触发信号或线程被中断。
 * <p>
 * 当线程等待事件并返回时，它会自动将事件设置为无信号状态。
 * <p>
 * 因此，如果事件以无信号状态启动，那么它允许传递的等待线程的数量等于通过set()发出信号的次数。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public class AutoResetEvent implements IResetEvent {

  private final Semaphore event;
  private final Integer mutex;

  public AutoResetEvent(boolean signalled) {
    event = new Semaphore(signalled ? 1 : 0);
    mutex = -1;

  }

  /**
   * Signal this event
   */
  @Override
  public void set() {
    synchronized (mutex) {
      if (event.availablePermits() == 0) {
        event.release();
      }
    }
  }

  /**
   * Set this event to the non-signalled state
   */
  @Override
  public void reset() {
    event.drainPermits();
  }


  /**
   * Wait for this event to become signalled. If several threads are waiting only one will be
   * allowed to pass each time that it is signalled. Waiting throws an exception if the thread is
   * interrupted.
   */
  @Override
  public void waitOne() {
    try {
      event.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  /**
   * Wait for this event to become signalled. If several threads are waiting only one will be
   * allowed to pass each time that it is signalled. Waiting throws an exception if the thread is
   * interrupted.
   */
  @Override
  public boolean waitOne(int timeout, TimeUnit unit) {
    try {
      return event.tryAcquire(timeout, unit);
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  /**
   * Is this event signalled.
   */
  @Override
  public boolean isSignalled() {
    return event.availablePermits() > 0;
  }
}

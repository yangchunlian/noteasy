package dowhat.is.right.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <English>
 * A single event which can be either signalled or non-signalled. Waiting for a signalled event does
 * not block. Waiting for a non-signalled event blocks until the event becomes signalled or the
 * thread is interrupted. When a thread waits for the event it does not affect the signalled status.
 * This must be modified manually.
 * <Chinese>
 * 可以发出信号或不发出信号的单一事件。
 * <p>
 * 等待一个信号事件不会阻塞。
 * <p>
 * 等待一个无信号的事件会阻塞，直到事件发出信号或线程被中断。
 * <p>
 * 当线程等待事件信号时，它不会影响信号状态。必须手动触发。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public class ManualResetEvent implements IResetEvent {

  private final Integer mutex;
  private volatile CountDownLatch event;

  public ManualResetEvent(boolean signalled) {
    mutex = -1;
    if (signalled) {
      event = new CountDownLatch(0);
    } else {
      event = new CountDownLatch(1);
    }
  }

  /**
   * Make this event signalled.
   * <p>
   * number - 1 = 0
   */
  @Override
  public void set() {
    event.countDown();
  }

  /**
   * make this event non-signalled
   * <p>
   * number + 1 != 0
   */
  @Override
  public void reset() {
    synchronized (mutex) {
      if (event.getCount() == 0) {
        event = new CountDownLatch(1);
      }
    }
  }

  /**
   * Wait for this event to become signalled.
   * <p>
   * Waiting throws an exception if the thread is interrupted.
   * <p>
   * synchronize method
   */
  @Override
  public void waitOne() {
    try {
      event.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  /**
   * Wait for this event to become signalled. Waiting throws an exception if the thread is
   * interrupted.
   * <p>
   * synchronize with a timeout
   */
  @Override
  public boolean waitOne(int timeout, TimeUnit unit) {
    try {
      return event.await(timeout, unit);
    } catch (InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  /**
   * Determine if this event is currently signalled.
   * <p>
   * check the event sign status
   *
   * @return bool
   */
  @Override
  public boolean isSignalled() {
    return event.getCount() == 0;
  }
}

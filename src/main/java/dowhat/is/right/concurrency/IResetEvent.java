package dowhat.is.right.concurrency;

import java.util.concurrent.TimeUnit;

/**
 * 时间重置接口
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public interface IResetEvent {

  /**
   * set a value
   */
  void set();

  /**
   * reset a value
   */
  void reset();

  /**
   * wait a event
   */
  void waitOne();

  /**
   * wait a event in a limit time
   *
   * @param timeout time
   * @param unit    unit
   * @return ok?
   */
  boolean waitOne(int timeout, TimeUnit unit);

  /**
   * check the event status
   *
   * @return is signalled
   */
  boolean isSignalled();
}

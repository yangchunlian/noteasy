package dowhat.is.right.zk;

import org.apache.zookeeper.KeeperException;

/**
 * @author 杨春炼
 * @since 2020-04-03
 */
@SuppressWarnings("seriral")
public class ZkException extends Exception {


  public enum Error {
    ZOOKEEPER_EXCEPTION("zk异常"),
    INTERRUPTED_EXCEPTION("中断异常"),
    LOCK_ALREADY_WAITING("锁等待被获取"),
    LOCK_ALREADY_ABANDONED("锁已经被遗弃"),
    LOCK_ALREADY_ACQUIRED("锁已经被获取"),
    LOCK_ALREADY_RELEASED("锁已经被释放"),
    LOCK_RELEASED_WHILE_WAITING("锁在等待时释放"),
    MAX_ATTEMPTS_EXCEEDED("超出获取次数"),
    UNKNOWN_ERROR("未知错误"),
    //
    ;
    //Chinese des.
    private String des;

    Error(String des) {
      this.des = des;
    }
  }

  private Error error;
  private KeeperException keeperException;

  public ZkException(KeeperException keeperException) {
    super();
    error = Error.ZOOKEEPER_EXCEPTION;
    this.keeperException = keeperException;
  }

  public ZkException(Error error) {
    this.error = error;
  }

  public Error getErrorCode() {
    return error;
  }

  public KeeperException getKeeperException() {
    return keeperException;
  }
}
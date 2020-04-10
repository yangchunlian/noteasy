package dowhat.is.right.zk;

/**
 * @author 杨春炼
 * @since 2020-04-03
 */
public class ZkValue extends ZkSyncPrimitive {

  ZkValue() throws InterruptedException {
    super(ZkSessionManager.instance());
  }
}

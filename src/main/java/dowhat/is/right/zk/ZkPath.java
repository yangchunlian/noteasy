package dowhat.is.right.zk;

import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.common.PathUtils;

/**
 * <English>
 * Create a path on ZooKeeper. First an attempt is made to create the target path directly. If this
 * fails because its immediate ancestor node does not exist, an attempt is made to create the
 * ancestor. This continues until an ancestor node is successfully created. Thereafter, successive
 * descendants are created until the target path is created. This algorithm improves performance in
 * most cases by minimizing round-trips to check the for the existence of ancestors of the target
 * path when the target or a close ancestor already exists.
 * <Chinese>
 * 在ZooKeeper上创建一个路径。
 * <p>
 * 首先尝试直接创建目标路径。
 * <p>
 * 如果因为它的直接祖先节点不存在而失败，则尝试创建祖先。
 * <p>
 * 这将一直进行下去，直到成功创建了一个祖先节点。
 * <p>
 * 然后，创建后续的后代，直到创建目标路径为止。
 * <p>
 * 在大多数情况下，此算法通过最小化往返来改进性能，以在目标或其近亲已经存在时检查目标路径的祖先是否存在。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public class ZkPath extends ZkSyncPrimitive {

  private final String targetPath;
  private final String[] pathNodes;
  private final CreateMode createMode;
  private int pathNodesIdx;
  /*
   * <English>
   * If create failed, they traverse create the parent path.
   *
   * <Chinese>
   * 如果创建失败，遍历的创建父路径，然后沿着父路径遍历创建子路径
   */
  private Runnable tryCreatePath = new Runnable() {
    @Override
    public void run() {
      String toCreate = "/";
      if (pathNodesIdx > 1) {
        StringBuilder currNodePath = new StringBuilder();
        for (int i = 1; i < pathNodesIdx; i++) {// i=1 to skip split()[0] empty node
          currNodePath.append("/");
          currNodePath.append(pathNodes[i]);
        }
        toCreate = currNodePath.toString();
      }
      zkClient()
          .create(toCreate, new byte[0], Ids.OPEN_ACL_UNSAFE, createMode, createPathHandler, this);
    }
  };
  private StringCallback createPathHandler = new StringCallback() {
    @Override
    public void processResult(int rc, String path, Object context, String name) {
      Code code = Code.get(rc);
      if (Code.OK == code || Code.NODEEXISTS == code) {
        if (pathNodesIdx >= pathNodes.length) {
          onStateUpdated();
          return;
        }
        pathNodesIdx++;
      } else {
        assert code == Code.NONODE;
        pathNodesIdx--;
      }
      tryCreatePath.run();
    }
  };

  public ZkPath(String targetPath) {
    this(targetPath, CreateMode.PERSISTENT);
  }

  public ZkPath(String targetPath, CreateMode createMode) {
    super(ZkSessionManager.instance());
    this.targetPath = targetPath;
    this.createMode = createMode;
    PathUtils.validatePath(targetPath);
    pathNodes = targetPath.split("/");
    pathNodesIdx = pathNodes.length;
    tryCreatePath.run();
  }

  public String getTargetPath() {
    return this.targetPath;
  }

  @Override
  public void process(WatchedEvent watchedEvent) {

  }
}

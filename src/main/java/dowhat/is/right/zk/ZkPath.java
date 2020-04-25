package dowhat.is.right.zk;

import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.common.PathUtils;

/**
 * <English>
 * Create a path on ZooKeeper.
 * <p>
 * First an attempt is made to create the target path directly.
 * <p>
 * If this fails because its immediate ancestor node does not exist,
 * <p>
 * an attempt is made to create the ancestor.
 * <p>
 * This continues until an ancestor node is successfully created.
 * <p>
 * Thereafter, successive descendants are created until the target path is created.
 * <p>
 * This algorithm improves performance in most cases by minimizing round-trips to check the for the
 * existence of ancestors of the target path when the target or a close ancestor already exists.
 *
 * <Chinese>
 * ZooKeeper创建路径。
 * <p>
 * 首先尝试直接创建目标路径。
 * <p>
 * 如果因为它的父节点不存在而失败，则尝试创建父节点。
 * <p>
 * 这将一直进行下去，直到创建成功第一个根节点。
 * <p>
 * 然后，逆向创建后代，直到创建成功目标路径。
 * <p>
 * 这个算法在目标节点或者目标节点的父节点已存在的情况下，会大大提升性能。
 *
 * @author 杨春炼
 * @since 2020-04-03
 */
public class ZkPath extends ZkSyncPrimitive {

  //想要创建的目标路径
  private String targetPath;
  //路径上的节点
  private String[] pathNodes;
  //路径是永久、暂时，是否有序
  private CreateMode createMode;
  //路径上节点的个数
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
      String toCreatePath = "/";
      if (pathNodesIdx > 1) {
        StringBuilder currNodePath = new StringBuilder();
        for (int i = 1; i < pathNodesIdx; i++) {// i=1 to skip split()[0] empty node
          currNodePath.append("/");
          currNodePath.append(pathNodes[i]);
        }
        toCreatePath = currNodePath.toString();
      }
      zkClient().create(
          toCreatePath, new byte[0], Ids.OPEN_ACL_UNSAFE, createMode, createPathHandler, this);
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

  ZkPath(String targetPath) {
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

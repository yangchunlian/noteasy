package dowhat.is.right.zk.lock;


import dowhat.is.right.zk.lock.ILock.LockType;

/**
 * @author 杨春炼
 * @since 2020-04-03
 */
public class ZkLockNode implements Comparable<ZkLockNode> {

  public final String name;
  public final LockType lockType;
  public final int seqNo;
  public final boolean self;


  /**
   * Retries the lock node id from a full lock node name path (the path after addition of sequence
   * number).
   *
   * @param localNodeName The name path to retrieve the id from
   * @return The lock node id
   */
  public static String getLockNodeIdFromName(String localNodeName) {
    int lastPathSep = localNodeName.lastIndexOf("/");
    if (lastPathSep != -1) {
      return localNodeName.substring(lastPathSep + 1);
    } else {
      return localNodeName;
    }
  }

  /**
   * <English>
   * Construct a lock node from a lock id. This can then be used in lock wait algorithms.
   *
   * @param lockId     The id of the lock node e.g. write-0000019
   * @param lockIdSelf The id of the lock performing the processing
   * @return A lock node wrapper object
   */
  public static ZkLockNode lockNodeFromId(String lockId, String lockIdSelf) {
    LockType lockType;
    if (lockId.startsWith(LockType.READ.toString())) {
      lockType = LockType.READ;
    } else if (lockId.startsWith(LockType.WRITE.toString())) {
      lockType = LockType.WRITE;
    } else {
      return null;
    }
    int seqNo;
    int sepIdx = lockId.lastIndexOf("-");
    if (sepIdx == -1) {
      return null;//not lock node
    }
    try {
      seqNo = Integer.parseInt(lockId.substring(sepIdx + 1));
    } catch (Exception e) {
      return null;// not lock node
    }
    return new ZkLockNode(lockId, lockType, seqNo, lockId.equals(lockIdSelf));
  }

  public ZkLockNode(String name, LockType lockType, int seqNo, boolean self) {
    this.name = name;
    this.lockType = lockType;
    this.seqNo = seqNo;
    this.self = self;
  }

  /**
   * The comparison function is designed so that any lock we need to wait for is sorted below ys in
   * the lock node queue.
   *
   * @param o zkLockNode
   * @return soft
   */
  @Override
  public int compareTo(ZkLockNode o) {
    if (seqNo < o.seqNo) {
      return -1;
    }
    return 1;
  }
}

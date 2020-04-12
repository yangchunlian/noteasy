package dowhat.is.right.dp.p3_singleton;

/**
 * @author 杨春炼
 * @since 2020-04-12
 */
public class LazySingletonDoubleCheckLocking {

  private volatile static LazySingletonDoubleCheckLocking instance;

  private LazySingletonDoubleCheckLocking() {
  }

  public static LazySingletonDoubleCheckLocking getInstance() {
    if (instance == null) {
      synchronized (LazySingletonDoubleCheckLocking.class) {
        if (instance == null) {
          instance = new LazySingletonDoubleCheckLocking();
        }
      }
    }
    return instance;
  }
}

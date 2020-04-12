package dowhat.is.right.dp.p3_singleton;

/**
 * @author 杨春炼
 * @since 2020-04-12
 */
public class LazySingletionSafe {

  private static LazySingletionSafe instance;

  private LazySingletionSafe() {
  }

  public static synchronized LazySingletionSafe getInstance() {
    if (instance == null) {
      instance = new LazySingletionSafe();
    }
    return instance;
  }
}

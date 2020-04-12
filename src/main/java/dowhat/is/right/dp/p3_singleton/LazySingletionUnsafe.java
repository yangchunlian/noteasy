package dowhat.is.right.dp.p3_singleton;

/**
 * @author 杨春炼
 * @since 2020-04-12
 */
public class LazySingletionUnsafe {

  private static LazySingletionUnsafe instance;

  private LazySingletionUnsafe() {
  }

  public static LazySingletionUnsafe getInstance() {
    if (instance == null) {
      instance = new LazySingletionUnsafe();
    }
    return instance;
  }
}

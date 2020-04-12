package dowhat.is.right.dp.p3_singleton;

/**
 * @author 杨春炼
 * @since 2020-04-12
 */
public class NotLazySingleton {

  private static NotLazySingleton instance = new NotLazySingleton();

  private NotLazySingleton() {
  }

  public static NotLazySingleton getInstance() {
    return instance;
  }
}

package dowhat.is.right.dp.p3_singleton;

/**
 * @author 杨春炼
 * @since 2020-04-12
 */
public class RegisterSingleton {

  private static class SingletonHolder {

    private static final RegisterSingleton INSTANCE = new RegisterSingleton();
  }

  private RegisterSingleton() {
  }

  public static RegisterSingleton getInstance() {
    return SingletonHolder.INSTANCE;
  }
}

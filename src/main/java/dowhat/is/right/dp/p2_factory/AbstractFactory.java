package dowhat.is.right.dp.p2_factory;

/**
 * 提供一个创建一系列相关或相互依赖对象的接口，而无需指定他们的具体的类。抽象工厂模式又称为kit模式，属于对象创建型模式。
 *
 * @author 杨春炼
 * @since 2020-04-12
 */
public abstract class AbstractFactory {

  abstract Tv createTv();

  abstract Pc createPc();

  public static void main(String[] args) {
    ProductFactory productFactory = new ProductFactory();
    Tv tv = productFactory.createTv();
    tv.open();
    tv.watch();
    Pc pc = productFactory.createPc();
    pc.work();
    pc.play();
  }
}

class ProductFactory extends AbstractFactory {

  @Override
  Tv createTv() {
    return new HaierTv();
  }

  @Override
  Pc createPc() {
    return new LenovoPc();
  }
}

interface Tv {

  void open();

  void watch();
}

class HaierTv implements Tv {

  @Override
  public void open() {
    System.out.println("open Haier");
  }

  @Override
  public void watch() {
    System.out.println("watch Haier");
  }
}

interface Pc {

  void work();

  void play();
}

class LenovoPc implements Pc {

  @Override
  public void work() {
    System.out.println("work");
  }

  @Override
  public void play() {
    System.out.println("play");
  }
}
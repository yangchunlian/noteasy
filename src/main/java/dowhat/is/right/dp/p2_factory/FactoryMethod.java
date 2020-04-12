package dowhat.is.right.dp.p2_factory;

import dowhat.is.right.dp.p2_factory.model.Man;
import dowhat.is.right.dp.p2_factory.model.People;
import dowhat.is.right.dp.p2_factory.model.Women;

/**
 * 通过定义一个抽象的核心工厂类，并定义创建产品对象的接口，创建具体产品实例的工作延迟到工厂子类去完成。
 * <p>
 * <优点>
 * <p>
 * 核心类只关注工厂类的接口定义，具体的产品实例交给具体的工厂子类去创建。
 * <p>
 * 当工厂需要增加一个产品时，无需修改现有的工厂代码。只需要增加一个具体产品类和其对应的工厂子类。
 * <p>
 * 使得系统的扩展性变得良好，符合面向对象编程的开闭原则。
 *
 * @author 杨春炼
 * @since 2020-04-12
 */
public class FactoryMethod {

  public static void main(String[] args) {
    CreatePeople peopleFactory = new FactoryMan();
    People people = peopleFactory.crate();
    people.say();
    peopleFactory = new FactoryWomen();
    people = peopleFactory.crate();
    people.say();
  }
}

interface CreatePeople {

  People crate();
}

class FactoryMan implements CreatePeople {

  @Override
  public People crate() {
    return new Man();
  }
}

class FactoryWomen implements CreatePeople {

  @Override
  public People crate() {
    return new Women();
  }
}
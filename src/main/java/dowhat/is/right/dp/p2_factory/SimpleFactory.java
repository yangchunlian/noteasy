package dowhat.is.right.dp.p2_factory;

import dowhat.is.right.dp.p2_factory.model.Man;
import dowhat.is.right.dp.p2_factory.model.People;
import dowhat.is.right.dp.p2_factory.model.Women;

/**
 * 简单工厂模式
 * <p>
 * <优点> 简单工厂模式最大的优点在于实现对象的创建和对象的使用分离，将对象的创建交给专门的工厂类负责。
 * <p>
 * <缺点> 工厂类不够灵活，增加新的具体产品需要修改工厂类的判断逻辑，而产品较多时，工厂方法代码逻辑将会非常复杂。
 * <p>
 *
 * @author 杨春炼
 * @since 2020-04-12
 */
public class SimpleFactory {

  public static People create(String type) {
    if ("man".equals(type)) {
      return new Man();
    } else if ("women".equals(type)) {
      return new Women();
    }
    throw new RuntimeException("err");
  }

  public static void main(String[] args) {
    People people = SimpleFactory.create("man");
    people.say();
    people = SimpleFactory.create("women");
    people.say();
  }
}


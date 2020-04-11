package dowhat.is.right.dp.p1_template;

/**
 * @author 杨春炼
 * @since 2020-04-11
 */
public class TemplateMethod {

  public static void main(String[] args) {
    AbstractClass concreteClass = new ConcreteClassA();
    concreteClass.templateMethod();
    concreteClass = new ConcreteClassB();
    concreteClass.templateMethod();
    concreteClass = new ConcreteClassC();
    concreteClass.templateMethod();
  }

  static abstract class AbstractClass {

    protected void step1() {
      System.out.println("AbstractClass:step1");
    }

    protected void step2() {
      System.out.println("AbstractClass:step2");
    }

    protected void step3() {
      System.out.println("AbstractClass:step3");
    }

    /**
     * 生命为final方法，避免子类复写
     */
    final void templateMethod() {
      this.step1();
      this.step2();
      this.step3();
    }
  }

  static class ConcreteClassA extends AbstractClass {

    @Override
    protected void step1() {
      System.out.println("ConcreteClassA:step1");
    }
  }

  static class ConcreteClassB extends AbstractClass {

    @Override
    protected void step2() {
      System.out.println("ConcreteClassB:step2");
    }
  }

  static class ConcreteClassC extends AbstractClass {

    @Override
    protected void step2() {
      System.out.println("ConcreteClassC:step2");
    }

    @Override
    protected void step3() {
      System.out.println("ConcreteClassC:step3");
    }
  }
}

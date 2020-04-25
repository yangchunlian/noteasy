package dowhat.is.right.jdk8.functional_programing_and_flow_computation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 函数式编程的更像是一种思想：
 * <p>
 * 1.他用final的方法解决并发问题（其实意味着有些避免不了的并发操作是无法使用函数式编程的）。
 * <p>
 * 2.函数式程序更加易于推断，因为他们有确定性。更加易于测试。没有副作用。
 * <p>
 * 3.函数式编程让符合和重新符合更加简单。
 * <p>
 * 翻译过来：把可以用函数式编程解决的模块，单独提出来，让代码更可读，更健壮，更方便维护。
 *
 * @author 杨春炼
 * @since 2020-04-11
 */
public class FunctionalFlow {

  /**
   * 简化程序
   */
  public static void simpleOneMethod() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        System.out.println("1");
        System.out.println("2");
      }
    }).start();
//    可简写为
    new Thread(() -> {
      System.out.println("1");
      System.out.println("2");
    }).start();
  }

  /**
   * 只能支持一个方法的接口
   */
  public interface InterfaceTest {

    String method1(String s);

//    void method2();
  }

  /**
   * 结论：通过合并现有代码来生成新功能而不是从头开始编写所有内容，我们可以更快地获得更可靠的代码。
   * <p>
   * 翻译一下：简洁、有逼格。
   */
  private static void onlySupportOneMethod() {
    InterfaceTest test = h -> h + "world!";
    System.out.println(test.method1("hello "));
    //相当于
    InterfaceTest test1 = new InterfaceTest() {
      @Override
      public String method1(String s) {
        return s + "world!";
      }
    };
    System.out.println(test1.method1("hello "));
  }

  /**
   * 用函数式编程和流式计算处理LIST
   * <p>
   * Collectors 提供了非常多且强大的API，可以将最终的数据收集成List、Set、Map，甚至是更复杂的结构(这三者的嵌套组合)。
   * <p>
   * Collectors 提供了很多API：
   * <p>
   * 数据收集：set、map、list
   * <p>
   * 聚合归约：统计、求和、最值、平均、字符串拼接、规约
   * <p>
   * 前后处理：分区、分组、自定义操作
   * <p>
   * 结论：提供了非常多且强大的API，流式操作改变并极大地提升了 Java 语言的可编程性。
   * <p>
   * 翻译过来：方便，提效。
   */
  private static void handleList() {
    List<BigDecimal> filter =
        Arrays.asList(new BigDecimal("111"), new BigDecimal("222"), new BigDecimal("333"));
    List<BigDecimal> filter1 = filter.stream().
        filter(a -> a.compareTo(new BigDecimal("200")) > 0).collect(Collectors.toList());
    System.out.println(filter1);
    //相当于
    List<BigDecimal> filter2 = new ArrayList<>();
    for (BigDecimal bigDecimal : filter) {
      if (bigDecimal.compareTo(new BigDecimal("200")) > 0) {
        filter2.add(bigDecimal);
      }
    }
    System.out.println(filter2);
  }

  /**
   * 纯函数式编程 Java 8 让函数式编程更简单，
   * <p>
   * lambda 表达式只能引用标记了 final 的外层局部变量，这就是说不能在 lambda 内部修改定义在域外的局部变量，否则会编译错误。
   */
  private static void noUse() {
    StringBuilder filterStr = new StringBuilder();
    List<BigDecimal> filter =
        Arrays.asList(new BigDecimal("111"), new BigDecimal("222"), new BigDecimal("333"));
    for (BigDecimal e : filter) {
      filterStr.append(e.toString());
    }
    Map<String, String> filterMap = new HashMap<>();
    filter.parallelStream().forEach(e -> {
      filterStr.append(e.toString());//这里会报错
      String value = filterMap.get("key") == null ? "" : filterMap.get("key");
      value += e.toString();
      filterMap.put("key", value);//这种处理还就不如用for循环了，取巧的方式虽然不报错但其实违背了forEach设计的初衷,且会有并发问题
    });
  }

  public static int fpTest(int a, int b) {
    //不会有异常 A/B这种就不行了
    //不依赖与任何外部设备，mysql、redis
    //传进来一个MAP，往里面赋值，就不行了，因为改变了外界数据
    while (b > 0) {
      a++;
      b--;
    }
    return a;
  }

  public static void main(String[] args) {
//    simpleOneMethod();
    onlySupportOneMethod();
  }
}

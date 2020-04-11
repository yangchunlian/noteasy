/**
 * <English>
 * Template Method Pattern
 * <p>
 * Define the skeleton of an algorithm in an operation,
 * <p>
 * deferring some steps to subclasses.
 * <p>
 * Template Method lets subclasses redefine certain steps of an algorithm without changing the
 * algorithm`s structure.
 *
 * <Chinese>
 * 模板设计模式
 * <p>
 * 蒂尼一个操作中的算法框架，
 * <p>
 * 而将一些步骤延迟到子类中。
 * <p>
 * 使得子类可以不改变一个算法的结构即可以冲顶以该算法的某些特定步骤。
 *
 * <des>
 * 实质上是封装了一个固定的流程，该流程由几个步骤组成，
 * <p>
 * 具体步骤可由子类进行不同的实现，从而让固定的流程产生不同的结果。
 * <p>
 * 该类其实是类的继承机制，但它却是一种应用非常广泛的设计模式。
 * <p>
 * 本质：抽象封装流程，进行具体实现。
 * <p>
 * <优点>
 * <li>优点1：封装不变，扩展可变。</li>
 * <li>优点2：框架流程由父类限定，子类无法更改；子类可以针对流程某些步骤进行具体实现。</li>
 * <p>
 * <缺点>
 * <li>抽象规定了行为，具体负责实现，与通常事物的行为相反，会带来理解上的困难。（父类调用了子类的方法）</li>
 *
 * @author 杨春炼
 * @since 2020-04-11
 */
package dowhat.is.right.dp.template_method;
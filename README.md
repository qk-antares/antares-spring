## 手写Spring

### 1. IoC容器

#### 1.1 概念介绍

##### 1.1.1 `BeanFactory`与`ApplicationContext`

Spring的核心就是能管理一组Bean，并能自动配置依赖关系的IoC容器。Spring中的IoC容器分为两类，`BeanFactory`和`ApplicationContext`，前者总是延迟创建Bean，而后者则在启动时初始化Bean。`ApplicationContext`的实际应用更加广泛，另一方面，`BeanFactory`的实现也要复杂得多，因此项目仅实现`ApplicationContext`。

##### 1.1.2 Bean的注入方式

我们了解下Spring中Bean的注入方式的发展历程。

###### XML配置方式

早期Spring容器采用XML来配置Bean，在配置文件中声明每个Bean的属性、构造函数参数、依赖等：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans 
           http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 定义一个 Bean -->
    <bean id="myBean" class="com.example.MyBean">
        <property name="name" value="Spring Bean"/>
    </bean>

</beans>
```

后期又加入了自动扫描包的功能，即通过`<context:component-scan base-package="org.example"/>`自动扫描 Spring 管理的组件（例如，带有`@Component`、`@Service`、`@Repository`和`@Controller`注解的类）的配置项。

###### 注解配置方式

从 Spring 2.5 开始，Spring 引入了基于注解的配置方式。这种方式使用注解标记 Bean 和依赖关系，简化了配置，减少了 XML 配置的冗余。

主要注解：
- `@Component`：将一个类声明为 Spring 管理的 Bean。
- `@Autowired`：自动注入依赖的 Bean。
- `@Value`：用于注入属性值。
- `@ComponentScan` 注解可以让 Spring 扫描特定包中的 Bean：

###### Java配置方式

作为XML配置的一种替代方案，通过`@Configuration`注解和`@Bean`注解的Java类来配置Bean，提供了更强的类型安全和灵活性。

```java
package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public MyBean myBean() {
        MyBean myBean = new MyBean();
        return myBean;  // 通过 Java 配置创建 MyBean 实例
    }
}
```

目前Spring Boot中99%都采用
`@ComponentScan`注解方式配置，因此，本项目仅实现Annotation配置+`@ComponentScan`扫描方式完成容器的配置。

从使用者的角度看，整个过程大概如下：

```java
//入口配置：
//标识哪个包下的Bean要被扫描
@ComponentScan
public class AppConfig {
}

//在扫描过程中，带有注解@Component的类，将被添加到IoC容器进行管理
@Component
public class Hello{
}

//引入第三方的Bean，可以通过在@Configuration工厂类中定义带@Bean的工厂方法
@Configuration
public class DbConfig {
    @Bean
    DataSource createDataSource(...) {
        return new HikariDataSource(...);
    }

    @Bean
    JdbcTemplate createJdbcTemplate(...) {
        return new JdbcTemplate(...);
    }
}
```

##### 1.1.3 Bean的作用域

Bean的作用域包括`Singleton`和`Prototype`等，而在实际使用大多数都采用`Singleton`，因此本项目只支持`Singleton`

#### 1.2 ResourceResolver

`@ComponentScan`可以**在指定包下扫描所有class**，我们实现的`ResourceResolver`就是来做这件事的。

给出一个包名，例如`org.example`，要扫描该包下的所有Class，实际上是在`classpath`中搜索所有文件，找出文件名匹配的`.class`文件。

> classpath: 在 Java 中，classpath 是一个用于指定类加载器如何查找和加载类和资源的位置的路径集合。简单来说，classpath 是 Java 程序在运行时用来查找 .class 文件和其他资源（如配置文件、图片等）的一个路径列表。

classpath 告诉 Java 虚拟机（JVM）以及其他 Java 工具（如编译器和调试器）在哪里查找 Java 类和资源文件。它可以包含目录、JAR 文件（Java ARchive）或其他归档文件。

定义


-----


可以用注解来修饰注解的定义吗？
例如：
@A
public @interfact B{}

此时@A起到一个什么作用？

这个问题问得很深入！✅ **是的，Java 是允许用注解来修饰另一个注解的定义的**。

你举的例子本质上就是在定义一个“**元注解**”（meta-annotation）行为 —— 即：**注解的注解**。

---

## ✅ 示例结构说明：

```java
@A
public @interface B {
}
```

这里：

- `@A` 是用来修饰另一个注解 `@B` 的注解
- `@B` 是一个自定义注解
- `@A` 在这里 **并不会直接作用于方法/类等业务代码**，而是作用于注解本身（即对 `@B` 起作用）

---

## 💡 那么 `@A` 到底起什么作用？

这取决于 `@A` 的功能。

---

### ✅ 场景一：标准元注解（你可能已经在用）

Java 内置了一些常用的注解就是专门**修饰其他注解**的，它们就是所谓的**“元注解”**：

| 注解         | 用途 |
|--------------|------|
| `@Retention` | 控制注解保留到哪一级别（源码 / class / 运行时） |
| `@Target`    | 控制注解能作用在哪些结构上（类、方法、字段等） |
| `@Documented`| 控制是否包含在 JavaDoc 中 |
| `@Inherited` | 控制子类是否继承注解 |

例如：

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface B {
}
```

这里 `@Retention` 和 `@Target` 就是修饰注解 `@B` 的行为的——它们就是 **元注解**（meta-annotations）。

---

### ✅ 场景二：自定义注解作为元注解（你提到的情况）

Java 并不限制 **只有元注解才能修饰注解**，你完全可以自定义一个注解 `@A`，然后用于修饰另一个注解 `@B`：

```java
// 定义注解 A
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE) // 注意这里！
public @interface A {
    String role() default "meta";
}

// 注解 B 被注解 A 修饰
@A(role = "custom behavior")
public @interface B {
}
```

### 🚨 注意关键点：
要让一个注解能修饰“其他注解”，它的 `@Target` 必须包含 `ElementType.ANNOTATION_TYPE`。

---

### ✅ 应用场景：框架/工具对注解做“注解驱动”处理

框架设计中，这种模式叫做 **“注解驱动注解”** 或 **“组合注解”**。Spring Boot 就大量用到：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Service {
}
```

上面这个 `@Service` 注解，其实是组合了 `@Component` 注解的行为，因此 Spring 会把带有 `@Service` 的类也当作组件进行自动扫描。

---

### ✅ 总结：

| 问题                             | 回答 |
|----------------------------------|------|
| 可以用注解修饰注解的定义吗？       | ✅ 可以 |
| 起什么作用？                     | 用于控制该注解的行为或为框架提供元信息 |
| 需要注意什么？                   | 要加 `@Target(ElementType.ANNOTATION_TYPE)` |
| 实际应用场景                     | 元注解、组合注解、注解驱动框架（如 Spring） |

---

需要我给你举个**Spring 风格组合注解 + 反射识别 + 自动注册的完整示例**吗？这种结构在大型系统设计中非常有用！

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

----

#### [1.2 ResourceResolver](https://github.com/qk-antares/antares-spring/blob/master/doc/IoC/1.2_ResourceResolver.md)

-----

#### [1.3 PropertyResolver](https://github.com/qk-antares/antares-spring/blob/master/doc/IoC/1.3_PropertyResolver.md)

---

#### [1.4 BeanDefinition](https://github.com/qk-antares/antares-spring/blob/master/doc/IoC/1.4_BeanDefinition.md)

#### [1.5 创建Bean实例与强依赖注入](https://github.com/qk-antares/antares-spring/blob/master/doc/IoC/1.5_BeanCreate&SInject.md)

#### [1.6 Bean 的弱依赖注入与初始化](https://github.com/qk-antares/antares-spring/blob/master/doc/IoC/1.6_BeanWInject&Init.md)

----

### [2. 相关工具包的知识](https://github.com/qk-antares/antares-spring/blob/master/doc/utils/2_Utils.md)

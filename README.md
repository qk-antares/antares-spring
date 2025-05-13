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

> `classpath`是一个用于指定类加载器如何查找和加载类和资源的位置的路径集合。简单来说，`classpath`是 Java 程序在运行时用来查找`.class`文件和其他资源（如配置文件、图片等）的一个路径列表。

##### 1.2.1 Resource

首先定义`Resource`类型标识classpath下的资源：
```java
public record Resource(String path, String name){
}
```

> `record`是Java14引入的一种新特性，提供了一种简洁的方式来定义**数据承载类**，简化定义包含多个字段的数据类，并自动生成常用方法，包括构造函数、`toString()`、`equals()`、`hashCode()`和字段访问器。
> 
> `record`类还有一些其他的特性：首先，`record`类型的对象默认是不可变的，也就是所有字段是`final`的；其次，`record`类不能继承其他类，因为它默认继承自`java.lang.Record`

##### 1.2.2 `scan()`方法

`ResourceResolver`中的`scan`方法用来获取扫描到的`Resource`：
```java
public class ResourceResolver {
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan(Function<Resource, R> mapper) {
        ...
    }
}
```

> 泛型是JDK5引入的新特性，编译器可以对泛型参数进行检测，从而提升代码的可读性以及稳定性。泛型的使用方式包括3种：
>
> 1.泛型类
>
> ```java
> public class Generic<T> {
>     private T key;
> }
> ```
>
> 2.泛型接口
>
> ```java
> public interface Generator<T> {
> 	public T method();
> }
> ```
>
> 在实现泛型接口时，可以指定或不指定类型
>
> ```java
> public class GeneratorImpl<T> implements Generator<T> {
> 	@Override
>     public T method() {
>         return null;
>     }
> }
> 
> public class GeneratorImpl<String> implements Generator<String> {
>     @Override
>     public String method() {
>         return "hello";
>     }
> }
> ```
>
> 3.泛型方法
>
> ```java
> public <E> void printArray(E[] inputArray) {
> 	for(E e : inputArray) {
>         sout(e);
>     }
> }
> ```
>
> 方法签名中的\<E\>代表一个占位符，它可以代表参数或返回值的类型，具体类型将在调用方法



> `Function<T, R>`是Java8引入的一个**函数式接口**，它代表一个接受一个输入参数T，返回一个结果R的函数。
>
> 函数式接口就是只包含一个（抽象）方法的接口，Java8引入了`@FunctionalInterface`注解来明确告诉编译器“这是一个函数式接口”
>
> ```java
> @FunctionalInterface
> public interface Comparator<T> {
>     int compare(T o1, T o2);
> }
> ```
>
> ❓为什么需要函数式接口
>
> 在Java8之前，写回调、策略、传递行为只能定义匿名内部类，代码非常冗长复杂：
>
> ```java
> button.addActionListener(new ActionListener() {
>     @Override
>     public void actionPerformed(ActionEvent e) {
>         System.out.println("Button clicked!");
>     }
> });
> ```
>
> 所谓匿名内部类，就是**没有名字的内部类**，**在定义的同时创建对象**，**一般用于临时实现接口或继承类**，只用一次（上面的`new ActionListener()`）。
>
> 而有了函数式接口后，就可以结合**Lambda表达式**使用，写成：
>
> ```java
> button.addActionListener(e -> System.out.println("Button clicked!"));
> ```

`scan`方法接受一个函数式接口`Function<Resource, R>`，即`scan`方法只定义发现资源文件`Resource`的逻辑，具体每个资源文件怎么处理，返回什么交给用户自定义，例如，下面的逻辑可以将**资源文件**映射为ClassName（类的全路径）：

```java
ResourceResolver rr = new ResourceResolver("org.example");
List<String> classList = rr.scan(res -> {
    String name = res.name(); // 资源名称"org/example/Hello.class"
    if (name.endsWith(".class")) { // 如果以.class结尾
        // 把"org/example/Hello.class"变为"org.example.Hello":
        return name.substring(0, name.length() - 6).replace(File.separator, ".");
    }
    // 否则返回null表示不是有效的ClassName:
    return null;
});
```

在`classpath`中扫描`Resource`时有几个注意点：

1. **不仅要搜索文件，还要支持搜索jar包**

    ```java
    // 通过ClassLoader获取URL列表:
    Enumeration<URL> en = getContextClassLoader().getResources(basePackagePath);
    while (en.hasMoreElements()) {
        URL url = en.nextElement();
        URI uri = url.toURI();
        String uriStr = removeLeadingSlash(uriToString(uri));
        String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
        if (uriBaseStr.startsWith("file:")) {
            uriBaseStr = uriBaseStr.substring(5);
        }
        if (uriBaseStr.startsWith("jar:")) {
            scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
        } else {
            scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
        }
    }
    ```

2. `ClassLoader`首先从`Thread.getContextClassLoader()`获取，如果获取不到，再从当前Class获取，因为**Web应用的ClassLoader不是JVM提供的基于Classpath的ClassLoader，而是Servlet容器提供的ClassLoader**，它不在默认的Classpath搜索，而是在`/WEB-INF/classes`目录和`/WEB-INF/lib`的所有jar包搜索，从`Thread.getContextClassLoader()`可以获取到Servlet容器专属的ClassLoader；

3. `URL`转为`URI`是必要的，转成 `URI` 后可以更方便、安全地与 `java.nio.file.Path`、`File` 等类型交互，`scanFile`中的`jarUriToPath`和`Paths.get`方法都会用到`URI`

-----

#### 1.3 PropertyResolver

Spring的注入分为`@Autowired`和`@Value`两种方式，前者是注入Bean，后者是注入属性值。我们实现的`PropertyResolver`用来实现保存配置项，以及对外提供查询功能，具体来说，它支持3种查询方式：
1. 按配置的`key`查询，如：`getProperty("jdbc.url")`
2. 以`${jdbc.url}`的方式查询，如：`getProperty("${jdbc.url}")`，后续可用于`@Value("${jdbc.url}")`注入
3. 带默认值的，例如`getProperty("${app.title:Spring}")`，用于`@Value("${app.title:Spring}")`注入

##### 1.3.1 构造函数

Java本身提供了`key-value`查询的`Properties`，因此在PropertyResolver中直接使用`Properties`作为构造函数的参数：

```java
public class PropertyResolver {

    Map<String, String> properties = new HashMap<>();

    public PropertyResolver(Properties props) {
        // 存入环境变量:
        this.properties.putAll(System.getenv());
        // 存入Properties:
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
    }
}
```

##### 1.3.2 `getter`

`PropertyResolver`通过其成员属性`Map<String, String> properties`来保存配置项，查询功能可以通过`Map`的`get`方法实现(`@Nullable`用于标记返回值可以是`null`)：

```java
@Nullable
public String getProperty(String key) {
    return this.properties.get(key);
}
```

##### 1.3.3 `${abc.xyz:default}`查询

对于`${abc.xyz:default}`的查询，首先定义`PropertyExpr`保存解析后的`key`和`defaultValue`:

```java
record PropertyExpr(String key, String defaultValue) {}
```

然后对`${...}`进行解析：

```java
PropertyExpr parsePropertyExpr(String expr) {
    if (expr.startsWith("${") && expr.endsWith("}")) {
        // 是否存在defaultValue
        int idx = expr.indexOf(":");
        if (idx == -1) {
            // 没有defaultValue(${key})
            String key = expr.substring(2, expr.length() - 1);
            return new PropertyExpr(key, null);
        } else {
            // 有defaultValue(${key:defaultValue})
            String key = expr.substring(2, idx);
            String defaultValue = expr.substring(idx + 1, expr.length() - 1);
            return new PropertyExpr(key, defaultValue);
        }
    }
    return null;
}
```

接下来把`getProperty()`改造下，即可支持`${...}`的查询：

```java
@Nullable
public String getProperty(String key) {
    // 解析${abc.xyz:defaultValue}:
    PropertyExpr keyExpr = parsePropertyExpr(key);
    if (keyExpr != null) {
        if (keyExpr.defaultValue() != null) {
            // 带默认值查询(需要看this.properties中是否存在了key):
            return getProperty(keyExpr.key(), keyExpr.defaultValue());
        } else {
            // 不带默认值查询(要求this.properties中必须存在key):
            return getRequiredProperty(keyExpr.key());
        }
    }
    // 普通key查询:
    String value = this.properties.get(key);
    if (value != null) {
        return parseValue(value);
    }
    return value;
}
```

##### 1.3.4 支持嵌套

为了支持嵌套的查询，形如：`${app.title:${APP_NAME:Spring}}`，可以递归的调用`parseValue`，这样的话我们实际上优先查找`app.title`，如果没有找到，再查找`APP_NAME`，如果都没有找到，则返回默认值`Spring`：

```java
//parseValue中又调用了getProperty，本质是递归
String parseValue(String value) {
    PropertyExpr valueExpr = parsePropertyExpr(value);
    if (valueExpr == null) {
        return value;
    }
    if (valueExpr.defaultValue() != null) {
        return getProperty(valueExpr.key(), valueExpr.defaultValue());
    } else {
        return getRequiredProperty(valueExpr.key());
    }
}
```

我们的实现不支持组合表达式（形如：`jdbc.url=jdbc:mysql//${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME}`）以及计算表达式（形如：`#{appBean.version() + 1}`）

##### 1.3.5 类型转换

`@Value`在注入时支持`boolean`、`int`、`Long`等基本类型与包装类型，Spring还支持`Date`、`Duration`等类型的注入。要实现类型转换，又不能写死。

先定义带类型转换的查询入口：

```java
@Nullable
public <T> T getProperty(String key, Class<T> clazz) {
    String value = getProperty(key);
    // 转换为指定类型
    return value == null ? null : convert(clazz, value);
}
```

再来实现`convert`方法，该方法的本质是将String类型转换为指定类型，使用函数式接口`Function<String, Object>`来表示这种转换：

```java
public class PropertyResolver {
    // 存储Class -> Function:
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    // 转换到指定Class类型:
    <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }
}
```

下一步是在`PropertyResolver`构造时，将各种转换器注册到`converters`中：

```java
public PropertyResolver(Properties props) {
    ...
    // String类型:
    converters.put(String.class, s -> s);
    // boolean类型:
    converters.put(boolean.class, s -> Boolean.parseBoolean(s));
    converters.put(Boolean.class, s -> Boolean.valueOf(s));
    // int类型:
    converters.put(int.class, s -> Integer.parseInt(s));
    converters.put(Integer.class, s -> Integer.valueOf(s));
    // 其他基本类型...
    // Date/Time类型:
    converters.put(LocalDate.class, s -> LocalDate.parse(s));
    converters.put(LocalTime.class, s -> LocalTime.parse(s));
    converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
    converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
    converters.put(Duration.class, s -> Duration.parse(s));
    converters.put(ZoneId.class, s -> ZoneId.of(s));
}
```

我们还可以对外暴漏一个`registerConverter`方法，允许用户添加自定义的转换器。

到这里，`PropertyResolver`就基本完成了对配置项的解析、查询、转换等功能，其使用过程如下：

```java
// Java标准库读取properties文件:
Properties props = new Properties();
props.load(fileInput); // 文件输入流
// 构造PropertyResolver:
PropertyResolver pr = new PropertyResolver(props);
// 后续代码调用...
// pr.getProperty("${app.version:1}", int.class)
```

上述从文件中读取`properties`的过程还可以通过读取`yaml`文件来实现，具体见`YamlUtils`

---

#### 1.4 BeanDefinition

##### 1.4.1 为什么需要BeanDefinition

接下来我们要开始正式实现管理Bean的IoC容器了。在IoC容器中，每个Bean都有唯一的`name`标识，Spring还允许一个Bean有多个`name`，这里我们简化成一个Bean只有一个`name`。很容易想到直接用`Map<String, Object>`来保存所有Bean：
```java
public class AnnotationConfigApplicationContext {
    Map<String, Object> beans;
}
```
但这么做的问题是丢失了Bean的定义信息，不便于我们创建Bean以及解析Bean的依赖关系。我们需要定义一个`BeanDefinition`类，它能够从`Annotation`中提取到足够的信息，便于后续创建Bean、设置依赖、调用初始化方法等：

```java
public class BeanDefinition {
    // 全局唯一的Bean Name:
    String name;

    // Bean的声明类型:
    Class<?> beanClass;

    // Bean的实例:
    Object instance = null;

    // 构造方法/null:
    Constructor<?> constructor;

    // 工厂方法名称/null:
    String factoryName;

    // 工厂方法/null:
    Method factoryMethod;

    // Bean的顺序:
    int order;

    // 是否标识@Primary:
    boolean primary;

    // init/destroy方法名称:
    String initMethodName;
    String destroyMethodName;

    // init/destroy方法:
    Method initMethod;
    Method destroyMethod;
}
```

##### 1.4.2 BeanDefinition的工作流程

对于带`@Component`的类，我们需要获取Class类型、构造方法，以及带有`@PostConstruct`和`@PreDestroy`注解的初始化与销毁方法，以及一些其他信息，例如`@Order`中定义的Bean的顺序（可用于AOP中切面的执行顺序）、`@Primary`标识等（多个相同类型时返回哪个Bean）。一个典型的定义如下：

```java
@Component
public class Hello {
    @PostConstruct
    void init() {}

    @PreDestroy
    void destroy() {}
}
```

对于带`@Configuration`的工厂类，它定义了一些带`@Bean`的工厂方法，我们需要获取工厂方法的返回值作为Class类型，方法本身作为Bean的`factoryMethod`，还需要收集`@Bean`中定义的`initMethod`和`destroyMethod`方法用于初始化与销毁，以及`@Order`、`@Primary`等信息。一个典型的定义如下：

```java
@Configuration
public class AppConfig {
    @Bean(initMethod="init", destroyMethod="close")
    DataSource createDataSource() {
        return new HikariDataSource(...);
    }
}
```

##### 1.4.3 按name获取Bean

通过查询`Map<String, BeanDefinition>`即可完成：

```java
public class AnnotationConfigApplicationContext {
    Map<String, BeanDefinition> beans;

    // 根据Name查找BeanDefinition，如果Name不存在，返回null
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }
}
```

##### 1.4.4 按class获取Bean

对于`@Component`定义的Bean，它的声明类型就是其实际类型；对于`@Bean`工厂方法定义的Bean，它的声明类型和实际类型不一定相同（例如上述`createDataSource()`定义的Bean，声明类型是`DataSource`，但实际类型却是`HikariDataSource`）。

在`BeanDefinition`中，`Class<?> beanClass`保存的是**声明类型**，而实际类型可以通过通过`instance.getClass()`来获取。

```java
public class BeanDefinition {
    // Bean的声明类型:
    Class<?> beanClass;

    // Bean的实例:
    Object instance = null;
}
```

对于按`class`获取Bean的`getBean(Class)`方法，我们需要遍历找出所有符合类型的Bean，如果不唯一，再判断`@Primary`，才能返回唯一Bean或报错：

```java
// 根据Type查找若干个BeanDefinition，返回0个或多个:
List<BeanDefinition> findBeanDefinitions(Class<?> type) {
    return this.beans.values().stream()
            // 过滤类型
            .filter(bean -> type.isAssignableFrom(bean.getBeanClass()))
            // 排序
            .sorted().collect(Collectors.toList());
}

// 根据Type查找某个BeanDefinition，如果不存在返回null，如果存在多个返回@Primary标注的一个:
@Nullable
public BeanDefinition findBeanDefinition(Class<?> type) {
    List<BeanDefinition> defs = findBeanDefinitions(type);
    if (defs.isEmpty()) { // 没有找到任何BeanDefinition
        return null;
    }
    if (defs.size() == 1) { // 找到唯一一个
        return defs.get(0);
    }
    // 多于一个时，查找@Primary:
    List<BeanDefinition> primaryDefs = defs.stream().filter(def -> def.isPrimary()).collect(Collectors.toList());
    if (primaryDefs.size() == 1) { // @Primary唯一
        return primaryDefs.get(0);
    }
    if (primaryDefs.isEmpty()) { // 不存在@Primary
        throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
    } else { // @Primary不唯一
        throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
    }
}
```

##### 1.4.5 获取所有的`BeanDefinition`

这个过程分为两步，第一步扫描所有Bean的`ClassName`，第二步是创建`BeanDefinitio`：

```java
public class AnnotationConfigApplicationContext {
    Map<String, BeanDefinition> beans;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        // 扫描获取所有Bean的Class类型:
        Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建Bean的定义:
        this.beans = createBeanDefinitions(beanClassNames);
    }
}
```

构造函数中的`Class<?> configClass`是入口类，相当于SpringBoot项目中的`xxxApplication`类。

###### 扫描`ClassName`

这个过程也可以细分为两个部分，第一部分是获取`@ComponentScan`注解配置的`package`，并利用`ResourceResolver`扫描该包下的所有`ClassName`；第二部分是查找`@Import`注解配置的类，这些类不位于`@ComponentScan`配置的包下，但依然需要注入IoC容器：

```java
/*
 * 扫描指定包下的所有Class，然后返回Class名字
 * 以及查找@Import注解修饰的类，这些类不位于@ComponentScan配置的包下，但依然可以注入IoC容器
 */
Set<String> scanForClassNames(Class<?> configClass) {
    // 获取@ComponentScan注解
    ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
    // 获取注解配置的package名字，未配置则默认当前类所在包
    String[] scanPackages = (scan == null || scan.value().length == 0)
            ? new String[] { configClass.getPackage().getName() }
            : scan.value();

    Set<String> classNameSet = new HashSet<>();
    for (String pkg : scanPackages) {
        // 扫描包下的所有Class
        var rr = new ResourceResolver(pkg);
        List<String> classList = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace(File.separator, ".");
            }
            return null;
        });
        classNameSet.addAll(classList);
    }

    // 查找@Import(Xyz.class)
    Import importConfig = configClass.getAnnotation(Import.class);
    if (importConfig != null) {
        for (Class<?> importConfigClass : importConfig.value()) {
            String importClassName = importConfigClass.getName();
            classNameSet.add(importClassName);
        }
    }

    return classNameSet;
}
```

###### 创建`BeanDefinition`

这个过程处理类上/类中方法上的`@Configuration`、`@Component`、`@Bean`、`@Primary`、`@Order`、`@PostConstruct`、`@PreDestroy`等注解及配置，提取出Bean的定义信息，创建`BeanDefinition`对象。

同样可以细分成两个部分，第一部分是创建被`@Component`修饰的类的`BeanDefinition`；第二部分进一步处理`@Configuration`这种工厂类（即其中包含`@Bean`注解的方法）：

```java
Map<String, BeanDefinition> createBeanDefinitions(Set<String> beanClassNames) {
    Map<String, BeanDefinition> defs = new HashMap<>();
    for (String className : beanClassNames) {
        // 获取class
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new BeanCreationException(e);
        }
        if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
            continue;
        }

        // 是否标注@Component，我们只注入@ComponentScan包下标注了@Component注解的类
        Component component = ClassUtils.findAnnotation(clazz, Component.class);
        if (component != null) {
            logger.atDebug().log("found component: {}", clazz.getName());
            // 获取类上的访问修饰符
            int mod = clazz.getModifiers();
            if (Modifier.isAbstract(mod)) {
                throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
            }
            if (Modifier.isPrivate(mod)) {
                throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
            }

            String beanName = ClassUtils.getBeanName(clazz);
            var def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz),
                    clazz.isAnnotationPresent(Primary.class),
                    // name of init / destroy method
                    null, null,
                    // init method
                    ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                    // destroy method
                    ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));

            addBeanDefinition(defs, def);
            logger.atDebug().log("define bean: {}", def);

            // 带有@Configuration注解的类作为工厂类，其中包含@Bean注解的方法
            Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
            if (configuration != null) {
                scanFactoryMethods(beanName, clazz, defs);
            }
        }
    }
    return defs;
}
```

需要注意的是，这里判断是否标注`@Component`，不是简单地判断类上是否有`@Component`，`@Component`是广义的，`@Service`、`@Repository`、`@Controller`等都是它的**子注解**：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    String value() default "";
}
```

> **注解的注解**称为**元注解**。
>
>  Java内置了一些标准元注解
>
> | 注解          | 用途                                            |
> | ------------- | ----------------------------------------------- |
> | `@Retention`  | 控制注解保留到哪一级别（源码 / class / 运行时） |
> | `@Target`     | 控制注解能作用在哪些结构上（类、方法、字段等）  |
> | `@Documented` | 控制是否包含在 JavaDoc 中                       |
> | `@Inherited`  | 控制子类是否继承注解                            |
>
> 对于自定义元注解，可以使用`@Target(ElementType.ANNOTATION_TYPE)`
>
> ```java
> @Retention(RetentionPolicy.RUNTIME)
> @Target(ElementType.ANNOTATION_TYPE)
> public @interface A {
>     String role() default "meta";
> }
> 
> @A(role = "custom behavior")
> public @interface B {
> }
> ```
>
> 使用`@Target(ElementType.TYPE)`也是可以的，它的范围更广，可以同时用于类、接口以及注解，像`@Component`就是用`@Target(ElementType.TYPE)`修饰的一种元注解

因此这里查找`@Component`注解实际上是一个递归的过程：

```java
public class ClassUtils {
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        A a = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (a != null) {
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
                    }
                    a = found;
                }
            }
        }
        return a;
    }
}
```

接着上面的总结，第二部分是处理`@Configuration`这种工厂类，其中包含`@Bean`注解的方法：

```java
void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
    for (Method method : clazz.getDeclaredMethods()) {
        // 是否带有@Bean标注:
        Bean bean = method.getAnnotation(Bean.class);
        if (bean != null) {
            // Bean的声明类型是方法返回类型:
            Class<?> beanClass = method.getReturnType();
            var def = new BeanDefinition(
                ClassUtils.getBeanName(method), beanClass,
                factoryBeanName,
                // 创建Bean的工厂方法:
                method,
                // @Order
                getOrder(method),
                // 是否存在@Primary标注?
                method.isAnnotationPresent(Primary.class),
                // init方法名称:
                bean.initMethod().isEmpty() ? null : bean.initMethod(),
                // destroy方法名称:
                bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                // @PostConstruct / @PreDestroy方法:
                null, null);
            addBeanDefinitions(defs, def);
        }
    }
}
```

因此对于下面的`DateTimeConfig`工厂类，我们实际上会创建3个`BeanDefinition`对象：`DateTimeConfig`、`LocalDateTime`和`ZonedDateTime`。

```java
@Configuration
public class DateTimeConfig {
    @Bean
    LocalDateTime local() { return LocalDateTime.now(); }

    @Bean
    ZonedDateTime zoned() { return ZonedDateTime.now(); }
}
```

`DateTimeConfig`的创建是必要的，因为后续我们需要调用`local()`和`zoned()`方法来创建`LocalDateTime`和`ZonedDateTime`的实例。

另外`BeanDefinition`同时存储了`initMethodName`和`initMethod`，以及`destroyMethodName`和`destroyMethod`，这是因为：
- 在`@Component`声明的Bean中，我们可以根据`@PostConstruct`和`@PreDestroy`直接拿到`Method`本身
- 在`@Bean`声明的Bean中，我们拿不到`Method`，只能从`@Bean`注解提取出字符串格式的方法名称

因此，存储在`BeanDefinition`的方法名称与方法，其中至少有一个为`null`。

----

### 2. 相关工具包的知识

#### 2.1 `slf4j`

`slf4j`是一个Java日志框架的**抽象层**，它提供了一套统一的日志API，但日志的实际输出由底层绑定的日志实现框架决定（如Logback、Log4j等）。

##### 2.1.1 配置slf4j的步骤（以Logback为例）

###### 添加pom依赖：
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.13</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.14</version>
</dependency>
```

###### 添加Logback配置文件：`logback.xml`（放在`resources/`）
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%d{HH:mm:ss}] [%level] [%logger{36}] - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 设置全局日志级别 -->
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

    <!-- 为特定包设置更详细的日志 -->
    <logger name="com.example" level="DEBUG"/>
</configuration>
```

##### 2.1.2 使用示例
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogExample {
    private static final Logger logger = LoggerFactory.getLogger(LogExample.class);

    public static void main(String[] args) {
        logger.atTrace().log("This is a TRACE message");
        logger.atDebug().log("This is a DEBUG message");
        logger.atInfo().log("This is an INFO message");
        logger.atWarn().log("This is a WARN message");
        logger.atError().log("This is an ERROR message");
    }
}
```
`logger.atDebug().log(...)` 只有在 `logger` 的级别是 `DEBUG` 或更细（`TRACE`） 时才会生效，其余同理
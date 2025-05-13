package com.antares.spring.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import jakarta.annotation.Nullable;

public class BeanDefinition implements Comparable<BeanDefinition> {
    // 全局唯一的Bean Name
    String name;

    // Bean的【声明类型】
    Class<?> beanClass;

    // Bean的实例
    Object instance = null;

    // 构造方法/null
    Constructor<?> constructor;

    // 工厂方法名称/null
    String factoryName;

    // 工厂方法
    Method factoryMethod;

    // Bean的顺序
    int order;

    // 是否标识@Primary
    boolean primary;

    // init/destroy方法名称
    String initMethodName;
    String destroyMethodName;

    // init/destroy方法
    Method initMethod;
    Method destroyMethod;

    /**
     * 这个构造方法用于带有@Component注解的Bean，这类Bean没有factoryName和factoryMethod
     * 而instance在实际创建Bean时才会被赋值
     * initMethodName和destroyMethodName为null(TODO 既然恒为null，我也不清楚作者为什么要传入这两个参数)
     * 
     * @param name
     * @param beanClass
     * @param constructor
     * @param order
     * @param primary
     * @param initMethodName
     * @param destroyMethodName
     * @param initMethod
     * @param destroyMethod
     */
    public BeanDefinition(String name, Class<?> beanClass, Constructor<?> constructor,
            int order, boolean primary, String initMethodName,
            String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        setInitAndDestoryMethodName(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    /**
     * 这个构造方法用于@Bean注解的Bean，这类Bean没有constructor
     * initMethod和destroyMethod为null(TODO 同上)
     * 
     * 
     * @param name
     * @param beanClass
     * @param factoryName
     * @param factoryMethod
     * @param order
     * @param primary
     * @param initMethodName
     * @param destroyMethodName
     * @param initMethod
     * @param destroyMethod
     */
    public BeanDefinition(String name, Class<?> beanClass, String factoryName, Method factoryMethod,
            int order, boolean primary, String initMethodName, String destroyMethodName,
            Method initMethod, Method destroyMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.constructor = null;
        this.factoryName = factoryName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestoryMethodName(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    /**
     * 设置BeanDefinition的init/destroy方法
     * 对于`@Component`声明的Bean，我们可以根据`@PostConstruct`和`@PreDestroy`直接拿到`Method`本身
     * 对于`@Bean`声明的Bean，我们拿不到`Method`，只能从`@Bean`注解提取出字符串格式的方法名称
     * 因此，存储在`BeanDefinition`的方法名称与方法，其中至少有一个为`null`。
     * 
     * @param initMethodName
     * @param destroyMethodName
     * @param initMethod
     * @param destroyMethod
     */
    private void setInitAndDestoryMethodName(String initMethodName, String destroyMethodName,
            Method initMethod, Method destroyMethod) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        if (initMethod != null) {
            initMethod.setAccessible(true);
        }
        if (destroyMethod != null) {
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }


    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Nullable
    public Object getInstance() {
        return instance;
    }

    @Nullable
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Nullable
    public String getFactoryName() {
        return factoryName;
    }

    @Nullable
    public Method getFactoryMethod() {
        return factoryMethod;
    }

    public int getOrder() {
        return order;
    }

    public boolean isPrimary() {
        return primary;
    }

    @Nullable
    public String getInitMethodName() {
        return initMethodName;
    }

    @Nullable
    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    @Nullable
    public Method getInitMethod() {
        return initMethod;
    }

    @Nullable
    public Method getDestroyMethod() {
        return destroyMethod;
    }

    @Override
    public String toString() {
        return "BeanDefinition [name=" + name + ", beanClass=" + beanClass.getName() + ", factory=" + getCreateDetail()
                + ", init-method="
                + (initMethod == null ? "null" : initMethod.getName()) + ", destroy-method="
                + (destroyMethod == null ? "null" : destroyMethod.getName())
                + ", primary=" + primary + ", instance=" + instance + "]";
    }

    String getCreateDetail() {
        if (this.factoryMethod != null) {
            String params = String.join(", ", Arrays.stream(this.factoryMethod.getParameterTypes())
                    .map(t -> t.getSimpleName()).toArray(String[]::new));
            return this.factoryMethod.getDeclaringClass().getSimpleName() + "." + this.factoryMethod.getName() + "("
                    + params + ")";
        }
        return null;
    }

    @Override
    public int compareTo(BeanDefinition def) {
        int cmp = Integer.compare(this.order, def.order);
        if (cmp != 0) {
            return cmp;
        }
        return this.name.compareTo(def.name);
    }
}

package com.antares.spring.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import jakarta.annotation.Nullable;

public class BeanDefinition {
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
     * TODO 为什么这里没有instance，factoryName/factoryMethod
     * 这个构造方法用于带有@Component注解的Bean
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
        // this.instance = instance;
        this.constructor = constructor;
        this.factoryName = null;
        this.factoryMethod = null;
        this.order = order;
        this.primary = primary;
        setInitAndDestoryMethodName(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

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
}

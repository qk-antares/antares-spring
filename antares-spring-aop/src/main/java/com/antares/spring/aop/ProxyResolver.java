package com.antares.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

public class ProxyResolver {
    ByteBuddy byteBuddy = new ByteBuddy();

    public <T> T createProxy(T bean, InvocationHandler handler) {
        // 目标Bean的Class类型
        Class<?> targetClass = bean.getClass();
        // 动态创建Proxy（targetClass的子类）
        Class<?> proxyClass = byteBuddy
                // 子类用默认无参构造方法
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // 拦截public方法
                .method(ElementMatchers.isPublic()).intercept(InvocationHandlerAdapter.of(
                        // 新的拦截器实例
                        new InvocationHandler() {
                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                // 调用原始Bean的方法
                                return handler.invoke(bean, method, args);
                            }
                        }))
                // 生成字节码
                .make()
                // 加载字节码
                .load(targetClass.getClassLoader()).getLoaded();

        // 创建Proxy实例
        Object proxy;
        try {
            // 使用无参构造方法创建实例
            proxy = proxyClass.getConstructor().newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return (T) proxy;
    }
}

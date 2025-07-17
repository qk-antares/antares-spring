package com.antares.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

public class ByteBuddyTest {
    public static void main(String[] args) throws Exception {
        // 创建真实对象
        RealSubject target = new RealSubject();

        // 使用ByteBuddy创建代理类
        Class<?> proxyClass = new ByteBuddy()
                .subclass(RealSubject.class, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                .method(ElementMatchers.isPublic())
                .intercept(InvocationHandlerAdapter.of(new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("Before method: " + method.getName());
                        Object result = method.invoke(target, args);
                        System.out.println("After method: " + method.getName());
                        return result;
                    }
                }))
                .make()
                .load(RealSubject.class.getClassLoader())
                .getLoaded();

        // 创建代理对象
        RealSubject proxy = (RealSubject) proxyClass.getConstructor().newInstance();
        
        // 调用代理方法
        proxy.request();
    }
}

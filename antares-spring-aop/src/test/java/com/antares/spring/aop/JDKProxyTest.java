package com.antares.spring.aop;

import java.lang.reflect.Proxy;

public class JDKProxyTest {
    public static void main(String[] args) {
        // 创建真实对象
        RealSubject target = new RealSubject();

        // 创建代理对象
        Subject proxySubject = (Subject) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class[]{Subject.class},
                (proxy, method, args1) -> {
                    System.out.println("Before method: " + method.getName());
                    Object result = method.invoke(target, args1);
                    System.out.println("After method: " + method.getName());
                    return result;
                }
        );

        // 调用代理方法
        proxySubject.request();
    }   
}

interface Subject {
    void request();
}

class RealSubject implements Subject {
    @Override
    public void request() {
        System.out.println("RealSubject request");
    }
}
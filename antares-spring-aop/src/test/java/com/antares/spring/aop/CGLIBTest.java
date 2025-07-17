package com.antares.spring.aop;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class CGLIBTest {
    public static void main(String[] args) {
        // 创建真实对象
        RealSubject target = new RealSubject();

        // 创建CGLIB增强器
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(RealSubject.class);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                System.out.println("Before method: " + method.getName());
                // 调用原始方法
                Object result = method.invoke(target, args);
                System.out.println("After method: " + method.getName());
                return result;
            }
        });

        // 创建代理对象
        RealSubject proxy = (RealSubject) enhancer.create();

        // 调用代理方法
        proxy.request();
    }
}

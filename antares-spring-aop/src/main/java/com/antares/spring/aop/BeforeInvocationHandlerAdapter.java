package com.antares.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public abstract class BeforeInvocationHandlerAdapter implements InvocationHandler {
    // before允许修改方法参数
    public abstract void before(Object proxy, Method method, Object[] args);

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before(proxy, method, args);
        return method.invoke(proxy, args);
    }
}

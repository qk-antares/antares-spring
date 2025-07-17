package com.antares.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/*
 * PoliteInvocationHandler只关系如何增强原始bean的方法
 * 它的invoke的参数是原始bean，而不是proxy
 * 因为Java标准库中的InvocationHandler恰好与拦截器的需求一致，所以直接使用了InvocationHandler而没有写新的接口，实际上两者是有区别的
 */
public class PoliteInvocationHandler implements InvocationHandler{

    @Override
    public Object invoke(Object bean, Method method, Object[] args) throws Throwable {
        //修改标记了@Polite注解的方法的返回值
        if (method.isAnnotationPresent(Polite.class)) {
            //获取原始方法的返回值
            String ret = (String) method.invoke(bean, args);
            //修改返回值
            if (ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1) + "!";
            }
            return ret;
        }

        //其他方法正常返回
        return method.invoke(bean, args);
    }
    
}

package com.antares.spring.aop.after;

import java.lang.reflect.Method;

import com.antares.spring.annotation.Component;
import com.antares.spring.aop.AfterInvocationHandlerAdapter;


@Component
public class PoliteInvocationHandler extends AfterInvocationHandlerAdapter {

    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        if (returnValue instanceof String s) {
            if (s.endsWith(".")) {
                return s.substring(0, s.length() - 1) + "!";
            }
        }
        return returnValue;
    }
}

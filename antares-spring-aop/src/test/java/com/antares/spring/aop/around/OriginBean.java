package com.antares.spring.aop.around;

import com.antares.spring.annotation.Around;
import com.antares.spring.annotation.Component;
import com.antares.spring.annotation.Value;

@Component
@Around("aroundInvocationHandler")
public class OriginBean {

    @Value("${customer.name}")
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}

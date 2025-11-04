package com.antares.spring.aop.before;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antares.spring.annotation.Around;
import com.antares.spring.annotation.Component;

@Component
@Around("logInvocationHandler")
public class BusinessBean {

    final Logger logger = LoggerFactory.getLogger(getClass());

    public String hello(String name) {
        logger.info("Hello, {}.", name);
        return "Hello, " + name + ".";
    }

    public String morning(String name) {
        logger.info("Morning, {}.", name);
        return "Morning, " + name + ".";
    }
}

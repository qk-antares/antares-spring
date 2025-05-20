package com.antares.spring.test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

@Component
public class B extends A {
    @PostConstruct
    public void initB() {
        System.out.println("B init");
    }

    @PreDestroy
    public void destroyB() {
        System.out.println("B destroy");
    }
    
}

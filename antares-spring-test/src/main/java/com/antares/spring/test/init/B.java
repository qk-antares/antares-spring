package com.antares.spring.test.init;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

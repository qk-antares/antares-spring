package com.antares.spring.test.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class A {
    @PostConstruct
    public void initA() {
        System.out.println("A init");
    }

    @PreDestroy
    public void destroyA() {
        System.out.println("A destroy");
    }
}

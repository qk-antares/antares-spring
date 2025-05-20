package com.antares.spring.test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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

package com.antares.spring.test.processor;

import org.springframework.stereotype.Component;

@Component
public class AttributeBean {
    String attibute = "default";

    public AttributeBean() {
        this.attibute = "processor";
        System.out.println("AttributeBean constructor");
    }

    @Override
    public String toString() {
        return this.attibute;
    }
}

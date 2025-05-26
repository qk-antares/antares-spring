package com.antares.spring.test.processor;

import org.springframework.stereotype.Component;

@Component
public class CommonBean1 {
    private final AttributeBean attributeBean;

    public CommonBean1(AttributeBean attributeBean) {
        this.attributeBean = attributeBean;
        System.out.println("CommonBean1 constructor, attibute: " + this.attributeBean);
    }

    @Override
    public String toString() {
        return "CommonBean1{" +
                "attibute=" + this.attributeBean +
                '}';
    }
}

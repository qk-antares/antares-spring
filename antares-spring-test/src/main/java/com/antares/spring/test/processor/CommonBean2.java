package com.antares.spring.test.processor;

import org.springframework.stereotype.Component;

@Component
public class CommonBean2 {
    private final AttributeBean attributeBean;

    public CommonBean2(AttributeBean attributeBean) {
        this.attributeBean = attributeBean;
        System.out.println("CommonBean2 constructor, attibute: " + this.attributeBean);
    }

    @Override
    public String toString() {
        return "CommonBean2{" +
                "attibute=" + this.attributeBean +
                '}';
    }
}

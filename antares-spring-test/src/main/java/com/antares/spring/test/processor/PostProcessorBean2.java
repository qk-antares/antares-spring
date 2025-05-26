package com.antares.spring.test.processor;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class PostProcessorBean2 implements BeanPostProcessor {
    private final AttributeBean attributeBean;

    public PostProcessorBean2(AttributeBean attributeBean) {
        this.attributeBean = attributeBean;
        System.out.println("MyPostProcessor constructor, attibute: " + this.attributeBean);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("MyPostProcessor.postProcessBeforeInitialization: " + beanName);
        return bean;
    }
}

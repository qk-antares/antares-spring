package com.antares.spring.test.processor;

import org.springframework.beans.factory.config.BeanPostProcessor;

// @Component
public class PostProcessorBean1 implements BeanPostProcessor {
    public PostProcessorBean1() {
        System.out.println("MyPostProcessor constructor");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("MyPostProcessor.postProcessBeforeInitialization: " + beanName);
        return bean;
    }
}

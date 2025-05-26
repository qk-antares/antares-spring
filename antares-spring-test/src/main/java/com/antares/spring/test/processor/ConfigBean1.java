package com.antares.spring.test.processor;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigBean1 {
    public ConfigBean1() {
        System.out.println("ConfigBean1 constructor");
    }
}

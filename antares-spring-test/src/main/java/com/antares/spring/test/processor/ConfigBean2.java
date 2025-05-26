package com.antares.spring.test.processor;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigBean2 {
    public ConfigBean2() {
        System.out.println("ConfigBean2 constructor");
    }
}

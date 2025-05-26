package com.antares.spring.test.processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestProcessor {
    /*
     * MyPostProcessor constructor
     * MyPostProcessor.postProcessBeforeInitialization: processorApp
     * AttributeBean constructor
     * MyPostProcessor.postProcessBeforeInitialization: attributeBean
     * CommonBean1 constructor, attibute: processor
     * MyPostProcessor.postProcessBeforeInitialization: commonBean1
     * CommonBean2 constructor, attibute: processor
     * MyPostProcessor.postProcessBeforeInitialization: commonBean2
     * AppConfig constructor, commonBean: CommonBean1{attibute=processor}
     * MyPostProcessor.postProcessBeforeInitialization: configBean1
     * ConfigBean2 constructor
     * MyPostProcessor.postProcessBeforeInitialization: configBean2
     */
    @Test
    void test() {
        System.out.println("TestProcessor");
        System.out.println("end");
    }
}

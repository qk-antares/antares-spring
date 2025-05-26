package com.antares.spring.test.init;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestBeanInit {
    @Autowired
    private B b;
    
    @Test
    void test() {
        System.out.println(b);
        System.out.println("end");
    }
}

package com.antares.spring.aop.after;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.ComponentScan;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class AfterApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}

package com.antares.spring.aop.before;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.ComponentScan;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class BeforeApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}

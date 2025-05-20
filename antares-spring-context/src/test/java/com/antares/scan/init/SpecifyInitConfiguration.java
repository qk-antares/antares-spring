package com.antares.scan.init;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.annotation.Value;

@Configuration
public class SpecifyInitConfiguration {

    @Bean(initMethod = "init")
    SpecifyInitBean createSpecifyInitBean(@Value("${app.title}") String appTitle, @Value("${app.version}") String appVersion) {
        return new SpecifyInitBean(appTitle, appVersion);
    }
}

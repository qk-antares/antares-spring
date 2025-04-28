package com.antares.scan.primary;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.annotation.Primary;

@Configuration
public class PrimaryConfiguration {

    @Primary
    @Bean
    DogBean husky() {
        return new DogBean("Husky");
    }

    @Bean
    DogBean teddy() {
        return new DogBean("Teddy");
    }
}

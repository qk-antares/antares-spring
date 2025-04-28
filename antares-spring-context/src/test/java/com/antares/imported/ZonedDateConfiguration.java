package com.antares.imported;

import java.time.ZonedDateTime;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Configuration;

@Configuration
public class ZonedDateConfiguration {
    @Bean
    ZonedDateTime startZonedDateTime() {
        return ZonedDateTime.now();
    }
}

package com.antares.imported;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Configuration;

@Configuration
public class LocalDateConfiguration {
    @Bean
    LocalDate startLocalDate() {
        return LocalDate.now();
    }

    @Bean
    LocalDateTime startLocalDateTime() {
        return LocalDateTime.now();
    }
}

package com.antares.hello;


import com.antares.spring.annotation.ComponentScan;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.annotation.Import;
import com.antares.spring.jdbc.JdbcConfiguration;
import com.antares.spring.web.WebMvcConfiguration;

@ComponentScan
@Configuration
@Import({ JdbcConfiguration.class, WebMvcConfiguration.class })
public class HelloConfiguration {

}

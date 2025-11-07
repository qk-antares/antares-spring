package com.antares.spring.web;

import java.util.Objects;

import com.antares.spring.annotation.Autowired;
import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.annotation.Value;

import jakarta.servlet.ServletContext;

@Configuration
public class WebMvcConfiguration {
    private static ServletContext servletContext = null;

    static void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    @Bean(initMethod = "init")
    ViewResolver viewResolver( //
            @Autowired ServletContext servletContext, //
            @Value("${spring.web.freemarker.template-path:/WEB-INF/templates}") String templatePath, //
            @Value("${spring.web.freemarker.template-encoding:UTF-8}") String templateEncoding) {
        return new FreeMarkerViewResolver(servletContext, templatePath, templateEncoding);
    }

    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext, "ServletContext is not set.");
    }
}

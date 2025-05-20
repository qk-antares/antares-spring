package com.antares.scan.init;

import com.antares.spring.annotation.Component;
import com.antares.spring.annotation.Value;

import jakarta.annotation.PostConstruct;

@Component
public class AnnotationInitBean {

    @Value("${app.title}")
    String appTitle;

    @Value("${app.version}")
    String appVersion;

    public String appName;

    @PostConstruct
    void init() {
        this.appName = this.appTitle + " / " + this.appVersion;
    }
}

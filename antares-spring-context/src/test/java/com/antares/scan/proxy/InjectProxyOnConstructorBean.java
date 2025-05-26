package com.antares.scan.proxy;

import com.antares.spring.annotation.Autowired;
import com.antares.spring.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {

    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}

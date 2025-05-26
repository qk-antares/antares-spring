package com.antares.scan.proxy;

import com.antares.spring.annotation.Autowired;
import com.antares.spring.annotation.Component;

@Component
public class InjectProxyOnPropertyBean {

    @Autowired
    public OriginBean injected;
}

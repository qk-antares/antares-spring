package com.antares.spring.aop.around;

import com.antares.spring.annotation.Autowired;
import com.antares.spring.annotation.Component;
import com.antares.spring.annotation.Order;

@Order(0)
@Component
public class OtherBean {

    public OriginBean origin;

    public OtherBean(@Autowired OriginBean origin) {
        this.origin = origin;
    }
}

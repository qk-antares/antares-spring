package com.antares.scan.nested;

import com.antares.spring.annotation.Component;

@Component
public class OuterBean {

    @Component
    public static class NestedBean {

    }
}

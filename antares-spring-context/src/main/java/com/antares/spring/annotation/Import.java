package com.antares.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定一些类，这些类不位于@ComponentScan配置的包下，但是需要被注入IoC容器
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Import {
    //需要被注入的类
    Class<?>[] value();
}

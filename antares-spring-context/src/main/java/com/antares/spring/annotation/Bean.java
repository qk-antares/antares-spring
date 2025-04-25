package com.antares.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Bean{
    //Bean name. 默认值是类名首字母小写
    String value() default "";

    // TODO initMethod和destroyMethod具体如何使用
    String initMethod() default "";

    String destroyMethod() default "";
}

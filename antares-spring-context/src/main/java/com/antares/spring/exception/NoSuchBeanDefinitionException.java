package com.antares.spring.exception;

/**
 * 根据Bean的name/type查找Bean时，未找到Bean的异常
 */
public class NoSuchBeanDefinitionException extends BeanDefinitionException {
    public NoSuchBeanDefinitionException() {
    }

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}

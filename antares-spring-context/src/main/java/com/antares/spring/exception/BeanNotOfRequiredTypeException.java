package com.antares.spring.exception;

/**
 * 尝试注入Bean时Name和Type不匹配
 */
public class BeanNotOfRequiredTypeException extends BeansException {
    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}

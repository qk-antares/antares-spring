package com.antares.spring.exception;

/**
 * 当根据Bean的name/type查找Bean时，找到多个符合条件的Bean，而又缺少唯一@Primary标注
 */
public class NoUniqueBeanDefinitionException extends BeanDefinitionException {
    public NoUniqueBeanDefinitionException() {
    }

    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }
}

package com.antares.spring.context;

public interface BeanPostProcessor {
    /**
     * 在Bean实例化之后调用
     *
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在bean.init()之后调用
     * 
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 在bean.setProperty()之前调用
     * 
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}

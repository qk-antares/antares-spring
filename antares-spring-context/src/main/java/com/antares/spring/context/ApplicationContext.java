package com.antares.spring.context;

import java.util.List;

public interface ApplicationContext extends AutoCloseable {
    /**
     * 是否存在指定name的Bean
     * 
     * @param name
     * @return
     */
    boolean containsBean(String name);

    /**
     * 获取指定name的Bean，找不到抛出NoSuchBeanDefinitionException异常
     * 
     * @param name
     * @return
     */
    <T> T getBean(String name);
    
    <T> T getBean(Class<T> requiredType);

    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 获取指定类型的一组Bean
     * 
     * @param requiredType
     * @return
     */
    <T> List<T> getBeans(Class<T> requiredType);

    /**
     * 关闭并执行所有Bean的销毁方法
     */
    void close();
}

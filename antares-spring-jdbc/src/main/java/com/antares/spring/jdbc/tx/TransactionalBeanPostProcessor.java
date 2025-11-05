package com.antares.spring.jdbc.tx;

import com.antares.spring.annotation.Transactional;
import com.antares.spring.aop.AnnotationProxyBeanPostProcessor;

public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {

}

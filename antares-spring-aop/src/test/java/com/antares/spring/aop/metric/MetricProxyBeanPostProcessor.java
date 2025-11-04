package com.antares.spring.aop.metric;

import com.antares.spring.annotation.Component;
import com.antares.spring.aop.AnnotationProxyBeanPostProcessor;

@Component
public class MetricProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Metric> {

}

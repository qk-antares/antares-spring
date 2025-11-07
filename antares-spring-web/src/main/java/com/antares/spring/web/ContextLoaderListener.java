package com.antares.spring.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antares.spring.context.AnnotationConfigApplicationContext;
import com.antares.spring.context.ApplicationContext;
import com.antares.spring.exception.NestedRuntimeException;
import com.antares.spring.io.PropertyResolver;
import com.antares.spring.web.utils.WebUtils;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ContextLoaderListener implements ServletContextListener {
    final Logger logger = LoggerFactory.getLogger(getClass());
    
    /*
     * Servlet容器启动时自动调用
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 创建IoC容器
        logger.info("init {}.", getClass().getName());
        var servletContext = sce.getServletContext();
        WebMvcConfiguration.setServletContext(servletContext);

        PropertyResolver propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${spring.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);

        ApplicationContext applicationContext = createApplicationContext(
                servletContext.getInitParameter("configuration"),
                propertyResolver);

        WebUtils.registerFilters(servletContext);
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);

        servletContext.setAttribute("applicationContext", applicationContext);
    }

    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        logger.info("init ApplicationContext by configuration: {}", configClassName);
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
                try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': " + configClassName);
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }
}

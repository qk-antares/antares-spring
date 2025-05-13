package com.antares.spring.context;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Component;
import com.antares.spring.annotation.ComponentScan;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.annotation.Import;
import com.antares.spring.annotation.Order;
import com.antares.spring.annotation.Primary;
import com.antares.spring.exception.BeanCreationException;
import com.antares.spring.exception.BeanDefinitionException;
import com.antares.spring.exception.NoUniqueBeanDefinitionException;
import com.antares.spring.io.PropertyResolver;
import com.antares.spring.io.ResourceResolver;
import com.antares.spring.utils.ClassUtils;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class AnnotationConfigApplicationContext {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, BeanDefinition> beans;
    protected final PropertyResolver propertyResolver;

    /**
     * 扫描指定的包下的所有Class，并创建BeanDefinition
     * 
     * @param configClass 入口类，对应与SpringBoot项目中的xxxApplication类
     * @param propertyResolver
     */
    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        
        // 扫描获取所有Bean的Class类型
        Set<String> beanClassNames = scanForClassNames(configClass);
        // 创建BeanDefinition
        this.beans = createBeanDefinitions(beanClassNames);
    }

    /**
     * 根据Name查找BeanDefinition，如果Name不存在返回null
     * 
     * @param name
     * @return
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    /**
     * 根据Type查找若干个BeanDefinition，返回0个或多个（只要声明类型符合）
     * 
     * @param type
     * @return
     */
    List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                // 过滤类型
                .filter(bean -> type.isAssignableFrom(bean.getBeanClass()))
                // 排序
                .sorted().collect(Collectors.toList());
    }

    /**
     * 根据Type查找某个BeanDefinition，如果不存在返回null，如果存在多个返回@Primary标注的一个
     * 
     * @param type
     * @return
     */
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if (defs.isEmpty()) {
            return null;
        }
        if (defs.size() == 1) {
            return defs.get(0);
        }
        // 多于一个时，查找@Parimary
        List<BeanDefinition> primaryDefs = defs.stream().filter(BeanDefinition::isPrimary).collect(Collectors.toList());
        if (primaryDefs.size() == 1) {
            return primaryDefs.get(0);
        }
        if (primaryDefs.isEmpty()) { // 不存在@Primary
            throw new NoUniqueBeanDefinitionException(
                    String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else { // @Primary不唯一
            throw new NoUniqueBeanDefinitionException(String
                    .format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

    /**
     * 扫描指定包下的所有Class，然后返回Class名字
     * 以及查找@Import注解修饰的类，这些类不位于@ComponentScan配置的包下，但依然可以注入IoC容器
     * 
     * @param configClass
     * @return
     */
    Set<String> scanForClassNames(Class<?> configClass) {
        // 获取@ComponentScan注解
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 获取注解配置的package名字，未配置则默认当前类所在包
        String[] scanPackages = (scan == null || scan.value().length == 0)
                ? new String[] { configClass.getPackage().getName() }
                : scan.value();
        logger.atInfo().log("component scan in packages: {}", Arrays.toString(scanPackages));

        Set<String> classNameSet = new HashSet<>();
        for (String pkg : scanPackages) {
            // 扫描包下的所有Class
            logger.atDebug().log("scan package: {}", pkg);
            var rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(res -> {
                String name = res.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace(File.separator, ".");
                }
                return null;
            });
            // 这里不使用logger.atDebug().log()，是为了防止lambda中重复判断日志等级降低效率
            if (logger.isDebugEnabled()) {
                classList.forEach((className) -> {
                    logger.debug("class found by @ComponentScan: {}", className);
                });
            }
            classNameSet.addAll(classList);
        }

        // 查找@Import(Xyz.class)
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                String importClassName = importConfigClass.getName();
                if (!classNameSet.contains(importClassName)) {
                    logger.atDebug().log("class found by @Import: {}", importClassName);
                    classNameSet.add(importClassName);
                } else {
                    logger.atWarn().log("ignore import: " + importClassName + " for it is already been scanned.");
                }
            }
        }

        return classNameSet;
    }

    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException(
                            "@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException(
                            "@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException(
                            "@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }

                Class<?> beanClass = method.getReturnType();
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException(
                            "@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName()
                            + " must not return primitive type.");
                }

                var def = new BeanDefinition(ClassUtils.getBeanName(method), beanClass, factoryBeanName, method,
                        getOrder(method),
                        method.isAnnotationPresent(Primary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null, null);
                addBeanDefinition(defs, def);
                logger.atDebug().log("define bean: {}", def);
            }
        }
    }

    Map<String, BeanDefinition> createBeanDefinitions(Set<String> beanClassNames) {
        Map<String, BeanDefinition> defs = new HashMap<>();
        for (String className : beanClassNames) {
            // 获取class
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }

            // 是否标注@Component，我们只注入@ComponentScan包下标注了@Component注解的类
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                logger.atDebug().log("found component: {}", clazz.getName());
                // 获取类上的访问修饰符
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }

                String beanName = ClassUtils.getBeanName(clazz);
                var def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz),
                        clazz.isAnnotationPresent(Primary.class),
                        // name of init / destroy method
                        null, null,
                        // init method
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // destroy method
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));

                addBeanDefinition(defs, def);
                logger.atDebug().log("define bean: {}", def);

                // 带有@Configuration注解的类作为工厂类，其中包含@Bean注解的方法
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    scanFactoryMethods(beanName, clazz, defs);
                }
            }
        }
        return defs;
    }

    /**
     * 获取合适的构造函数
     * 首先会尝试获取public的构造函数，如果不存在，尝试获取其他修饰符的构造函数
     * 构造函数应该唯一
     * 
     * @param clazz
     * @return
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (cons.length != 1) {
            throw new BeanDefinitionException(
                    "More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }

    void addBeanDefinition(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }

    /**
     * 获取类上的@Order
     * | @Order(100)
     * | @Component
     * | public class Hello{}
     * 
     * @param clazz
     * @return
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 类似地，获取工厂方法上的@Order
     * | @Order(100)
     * | @Bean
     * | Hello createHello() {return new Hello();}
     * 
     * @param method
     * @return
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }
}

package com.antares.spring.context;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antares.spring.annotation.Autowired;
import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Component;
import com.antares.spring.annotation.ComponentScan;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.annotation.Import;
import com.antares.spring.annotation.Order;
import com.antares.spring.annotation.Primary;
import com.antares.spring.annotation.Value;
import com.antares.spring.exception.BeanCreationException;
import com.antares.spring.exception.BeanDefinitionException;
import com.antares.spring.exception.BeanNotOfRequiredTypeException;
import com.antares.spring.exception.NoSuchBeanDefinitionException;
import com.antares.spring.exception.NoUniqueBeanDefinitionException;
import com.antares.spring.exception.UnsatisfiedDependencyException;
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

    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    private Set<String> creatingBeanNames;

    /**
     * 扫描指定的包下的所有Class，并创建BeanDefinition
     * 
     * @param configClass      入口类，对应与SpringBoot项目中的xxxApplication类
     * @param propertyResolver
     */
    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;

        // 扫描获取所有Bean的Class类型
        Set<String> beanClassNames = scanForClassNames(configClass);
        // 创建BeanDefinition
        this.beans = createBeanDefinitions(beanClassNames);

        // 处理循环依赖
        this.creatingBeanNames = new HashSet<>();

        // 首先创建@Configuration类型的Bean
        this.beans.values().stream()
                .filter(this::isConfigurationBean).sorted().map(def -> {
                    // 创建Bean示例
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                }).collect(Collectors.toList());

        // 创建BeanPostProcessor类型的Bean
        List<BeanPostProcessor> processors = this.beans.values().stream()
                .filter(this::isBeanPostProcessorDefinition)
                .sorted().map(def -> (BeanPostProcessor) createBeanAsEarlySingleton(def))
                .collect(Collectors.toList());
        this.beanPostProcessors.addAll(processors);

        // 创建其他普通Bean(@Component)
        List<BeanDefinition> defs = this.beans.values().stream()
                // 过滤出instance为null的BeanDefinition
                .filter(def -> def.getInstance() == null)
                .sorted().collect(Collectors.toList());
        // 依次创建Bean实例
        defs.forEach(def -> {
            // 如果Bean未被创建(可能在其他Bean的构造方法注入前被创建)
            if (def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
        });

        if (logger.isDebugEnabled()) {
            this.beans.values().stream().sorted().forEach(def -> {
                logger.debug("bean initialized: {}", def);
            });
        }

        // Filed注入与Setter注入
        this.beans.values().forEach(def -> {
            injectBean(def);
        });

        // 调用init方法
        this.beans.values().forEach(def -> {
            initBean(def);
        });
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
                // 排序，根据Order，Order相同比较name的字典序，BeanDefinition需要实现Comparable接口
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
     * 根据Name和Type查找BeanDefinition，如果Name不存在返回null，Name存在但Type不匹配抛出异常
     * 
     * @param name
     * @param requiredType
     * @return
     */
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(
                    String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.",
                            requiredType.getName(), name, def.getBeanClass().getName()));
        }
        return def;
    }

    /**
     * 根据name查找Bean实例，不存在时抛出异常NoSuchBeanDefinitionException
     * 
     * @param <T>
     * @param name
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 根据Type查找Bean实例，如果不存在抛出异常NoSuchBeanDefinitionException
     * 存在多个但缺少唯一@Primarily标注抛出NoUniqueBeanDefinitionException
     * 
     * @param <T>
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(
                    String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
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

    boolean isConfigurationBean(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    /**
     * 创建一个Bean，然后使用BeanPostProcessor处理，但不进行字段和方法级别的注入
     * 字段注入形如：
     * | @Autowired
     * | JdbcTemplate jdbcTemplate;
     * 方法注入形如：
     * | JdbcTemplate jdbcTemplate;
     * | @Autowired
     * | void setJdbcTemplate(JdbcTemplate jdbcTemplate) {this.jdbcTemplate =
     * jdbcTemplate;}
     * 
     * 如果创建的Bean不是@Configuration/BeanPostProcessor，则其在构造方法中依赖的Bean会被自动创建
     * 
     * @param def
     * @return
     */
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        logger.atDebug().log("Try create bean '{}' as early singleton: {}",
                def.getName(), def.getBeanClass().getName());
        if (!this.creatingBeanNames.add(def.getName())) {
            throw new UnsatisfiedDependencyException(
                    String.format("Circular dependency detected when create bean {%s}", def.getName()));
        }

        // 创建实例：通过构造函数或工厂方法
        Executable createFn = null;
        if (def.getFactoryName() == null) {
            // 通过构造函数创建
            createFn = def.getConstructor();
        } else {
            // 通过工厂方法创建
            createFn = def.getFactoryMethod();
        }

        // 构造函数或工厂的参数及注解
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parameterAnnos = createFn.getParameterAnnotations();
        // 存储注入的结果
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            final Annotation[] paramAnnos = parameterAnnos[i];
            // 进行注入的有两类注解，@Autowired和@Value
            final Value value = ClassUtils.findAnnotation(paramAnnos, Value.class);
            final Autowired autowired = ClassUtils.findAnnotation(paramAnnos, Autowired.class);

            // TODO 不理解：@Configuraion类型的Bean是工厂，不允许使用@Autowired
            final boolean isConfiguration = isConfigurationBean(def);
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.",
                                def.getName(), def.getBeanClass().getName()));
            }

            // TODO 不理解：BeanPostProcessor不能依赖其他Bean，不允许使用@Autowired创建:
            final boolean isBeanPostProcessor = isBeanPostProcessorDefinition(def);
            if (isBeanPostProcessor && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create BeanPostProcessor '%s': %s.",
                                def.getName(), def.getBeanClass().getName()));
            }

            // 参数上存在@Autowired和@Value之一
            if (autowired != null && value != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.",
                                def.getName(), def.getBeanClass().getName()));
            }
            if (autowired == null && value == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(),
                                def.getBeanClass().getName()));
            }

            // 参数类型
            final Class<?> type = parameter.getType();
            if (value != null) {
                // @Value注解
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else {
                // @Autowired注解
                String name = autowired.name();
                boolean required = autowired.required();
                // 依赖的BeanDefinition：如果不指定name，则优先匹配类型，如果指定了name，则name和type都要匹配
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type)
                        : findBeanDefinition(name, type);
                // 找不到所依赖Bean的BeanDefinition（注意不是实例）
                if (required && dependsOnDef == null) {
                    throw new BeanCreationException(
                            String.format("Missing autowired bean with type '%s' when create bean '%s': %s.",
                                    type.getName(), def.getName(), def.getBeanClass().getName()));
                }

                if (dependsOnDef != null) {
                    // 获取依赖的Bean
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    // 依赖的Bean尚未实例化
                    // TODO
                    // 前面已经判断了，如果isConfigurationBean，且@Autowired时抛出异常，按道理这里不用再判断!isConfiguration
                    if (autowiredBeanInstance == null && !isConfiguration) {
                        // 递归创建
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }

                    args[i] = dependsOnDef.getInstance();
                } else {
                    args[i] = null;
                }
            }
        }

        // 至此我们获取了构造函数中参数的具体实例，接下来创建实例
        Object instance = null;
        if (def.getFactoryName() == null) {
            // 通过构造函数创建
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(
                        String.format("Exception when create bean '%s': %s", def.getName(),
                                def.getBeanClass().getName()),
                        e);
            }
        } else {
            // 通过@Bean的工厂方法创建，首先获取工厂实例
            Object factoryInstance = getBean(def.getFactoryName());
            try {
                instance = def.getFactoryMethod().invoke(factoryInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(
                        String.format(String.format("Exception when create bean '%s': %s",
                                def.getName(), def.getBeanClass().getName()), e));
            }
        }
        def.setInstance(instance);

        // 调用BeanPostProcessor的postProcessBeforeInitialization方法
        for (BeanPostProcessor processor : this.beanPostProcessors) {
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (processed == null) {
                throw new BeanCreationException(
                        String.format("PostBeanProcessor returns null when process bean '%s' by %s",
                                def.getName(), processor));
            }
            // 如果一个BeanPostProcessor替换了原始的Bean实例，则更新instance
            if (processed != def.getInstance()) {
                logger.atDebug().log("Bean '{}' was replaced by post processor {}.",
                        def.getName(), processor.getClass().getName());
                def.setInstance(processed);
            }
        }

        return def.getInstance();
    }

    /**
     * 对单个Bean进行Field注入和Setter注入
     * 
     * @param def
     */
    void injectBean(BeanDefinition def) {
        // 获取Bean实例，或被代理的原始实例:
        final Object beanInstance = getProxiedInstance(def);
        try {
            injectProperties(def, def.getBeanClass(), beanInstance);
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    /**
     * Field注入和Setter注入，并递归地对通过父类继承的Field和Method进行注入
     * 
     * @param def
     * @param clazz
     * @param instance
     * @param def
     * @param clazz
     * @param instance
     * @throws ReflectiveOperationException
     */
    void injectProperties(BeanDefinition def, Class<?> clazz, Object instance) throws ReflectiveOperationException {
        // 在当前类中查找Field和Method并注入
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperty(def, clazz, instance, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperty(def, clazz, instance, m);
        }
        // 在父类中查找Field和Method尝试注入
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            injectProperties(def, superClazz, instance);
        }
    }

    /**
     * 尝试注入单个属性(Field注入/Setter注入)
     * 
     * @param def
     * @param clazz
     * @param instance
     * @param acc      Field/Method
     * @throws ReflectiveOperationException
     */
    void tryInjectProperty(BeanDefinition def, Class<?> clazz, Object instance, AccessibleObject acc)
            throws ReflectiveOperationException {
        // 获取Field/Method上的@Value和@Autowired
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        if (value == null && autowired == null) {
            return;
        }

        // 获取@Value/@Autowired所修饰的Field/Method
        Field field = null;
        Method method = null;
        // 这里是JDK 16的语法糖
        if (acc instanceof Field f) {
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method m) {
            checkFieldOrMethod(m);
            // setter只能是1个参数
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s",
                                m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessileType = field != null ? field.getType() : method.getParameterTypes()[0];

        // 不能同时存在@Value和@Autowired
        if (value != null && autowired != null) {
            throw new BeanCreationException(
                    String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                            clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        // @Value注入
        if (value != null) {
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessileType);
            if (field != null) {
                logger.atDebug().log("Field injection: {}.{} = {}",
                        def.getBeanClass().getName(), accessibleName, propValue);
                field.set(instance, propValue);
            }
            if (method != null) {
                logger.atDebug().log("Setter injection: {}.{}({})",
                        def.getBeanClass().getName(), accessibleName, propValue);
                method.invoke(instance, propValue);
            }
        }

        // @Autowired注入
        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.required();
            Object depends = name.isEmpty() ? getBean(accessileType) : findBeanDefinition(name, accessileType);
            if (required && depends == null) {
                throw new UnsatisfiedDependencyException(
                        String.format("Dependency bean not found when inject %s.%s for bean '%s': %s",
                                clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (depends != null) {
                if (field != null) {
                    logger.atDebug().log("Field injection: {}.{} = {}",
                            def.getBeanClass().getName(), accessibleName, depends);
                    field.set(instance, depends);
                }
                if (method != null) {
                    logger.atDebug().log("Setter injection: {}.{}({})",
                            def.getBeanClass().getName(), accessibleName, depends);
                    method.invoke(instance, depends);
                }
            }
        }
    }

    /**
     * 检查Field/Method的访问修饰符，不能是static和final
     * 
     * @param m
     */
    void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new BeanDefinitionException(
                    String.format("Field '%s' in class '%s' must not be static.",
                            m.getName(), m.getDeclaringClass().getName()));
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field) {
                throw new BeanDefinitionException(
                        String.format("Field '%s' in class '%s' must not be final.",
                                m.getName(), m.getDeclaringClass().getName()));
            }
            // TODO 为什么setter注入的方法不能用final修饰
            if (m instanceof Method) {
                logger.atWarn().log(
                        "Inject final method '%s' in class '%s' should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.",
                        m.getName(), m.getDeclaringClass().getName());
            }
        }
    }

    /**
     * 调用init方法
     * 
     * @param def
     */
    void initBean(BeanDefinition def) {
        callMethod(def.getInstance(), def.getInitMethod(), def.getInitMethodName());
    }

    /**
     * 调用init/destroy方法
     * 
     * 这里的method不用setAccessible(true)，因为我们已经在创建Bean时调用了setAccessible(true)
     * 
     * @param def
     */
    private void callMethod(Object instance, Method method, String methodName) {
        // 调用init/destroy方法
        if (method != null) {
            try {
                method.invoke(instance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        } else if (methodName != null) {
            // 查找initMethod/destroyMethod="xyz"
            Method namedMethod = ClassUtils.getNamedMethod(instance.getClass(), methodName);
            namedMethod.setAccessible(true);
            try {
                namedMethod.invoke(instance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        }
    }

    boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }

    private Object getProxiedInstance(BeanDefinition def) {
        Object beanInstance = def.getInstance();
        // 如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean(一般是beanInstanceProxy中的一个属性)
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
            if (restoredInstance != beanInstance) {
                logger.atDebug().log("BeanPostProcessor {} specified injection from {} to {}.",
                        beanPostProcessor.getClass().getSimpleName(), beanInstance.getClass().getSimpleName(),
                        restoredInstance.getClass().getSimpleName());
                beanInstance = restoredInstance;
            }
        }
        return beanInstance;
    }
}

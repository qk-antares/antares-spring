package com.antares.spring.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.antares.spring.annotation.Bean;
import com.antares.spring.annotation.Component;
import com.antares.spring.exception.BeanDefinitionException;

import jakarta.annotation.Nullable;

public class ClassUtils {
    /**
     * 递归查找Annotation A
     * 
     * 有以下两种情况：
     * 情况一：Annotation A直接标注在Class上
     * | @A
     * | public class Hello{}
     * | findAnnotation(Hello.class, A.class); ✅ 返回 A 注解对象
     * 
     * 情况二：Annotation A标注在Annotation B上，Annotation B标注在Class上。此时Annotation
     * A相当于元注解
     * 
     * | @A
     * | public @interfact B{}
     * | @B
     * | public class Hello{}
     * | findAnnotation(Hello.class, A.class); // ✅ 返回 A 注解对象
     * 
     * | @A
     * | public @interface B {}
     * | @A
     * | public @interface C {}
     * | @B
     * | @C
     * | public class Hello {}
     * | findAnnotation(Hello.class, A.class); // ❌ 抛出异常：Duplicate @A found on
     * | class Hello
     * 
     * @param <A>
     * @param target
     * @param annoClass
     * @return
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        A a = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (a != null) {
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class "
                                + target.getSimpleName());
                    }
                    a = found;
                }
            }
        }
        return a;
    }

    /**
     * 从一个Annotation数组中查找指定的Annotation
     * 当isInstance用于Annotation时，只能是注解类型的完全匹配，因为注解不存在继承结构
     * 
     * @param <A>
     * @param annos
     * @param annoClass
     * @return
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <A extends Annotation> A findAnnotation(Annotation[] annos, Class<A> annoClass) {
        for(Annotation anno : annos) {
            if(annoClass.isInstance(anno)) {
                return (A) anno;
            }
        }
        return null;
    }

    /**
     * 获取Bean Name。首选是@Component注解的value值，其次是类名首字母小写
     * 
     * @param clazz
     * @return
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        // 查找@Component
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            // @Component注解存在
            name = component.value();
        } else {
            // 未找到@Component，继续在其他注解中查找@Component
            for (Annotation anno : clazz.getAnnotations()) {
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        // 通过反射机制获取value
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value", e);
                    }
                }
            }
        }

        if (name.isEmpty()) {
            // @Component注解不存在，使用类名首字母小写
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        return name;
    }

    /**
     * 获取工厂方法的BeanName，默认依然是@Bean注解的value值，其次是方法名
     * @param method
     * @return
     */
    public static String getBeanName(Method method) {
        Bean bean = method.getAnnotation(Bean.class);
        String name = bean.value();
        return name.isEmpty() ? method.getName() : name;
    }

    /**
     * 查找带有@PostConstruct或者@PreDestroy注解的方法
     * | @PostConstruct
     * | public void init(){}
     * 
     * @param clazz
     * @param annoClass
     * @return
     */
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {
        List<Method> ms = Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(annoClass)).map(m -> {
            if (m.getParameterCount() != 0) {
                throw new BeanDefinitionException(String.format("Method '%s' with @%s must not have argument: %s", m.getName(), annoClass.getSimpleName(), clazz.getName()));
            }
            return m;
        }).collect(Collectors.toList());

        if(ms.isEmpty()) {
            return null;
        }
        if(ms.size() == 1) {
            return ms.get(0);
        }   
        throw new BeanDefinitionException(String.format("Multiple method with @%s found on class: %s", annoClass.getSimpleName(), clazz.getName()));
    }
}

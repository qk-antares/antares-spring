package com.antares.spring.io;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Nullable;

/*
 * 保存配置项并对外提供查询功能
 * 
 * 支持的：
 * 1. 按配置的key查询，例如：getProperty("app.title");
 * 2. 以${abc.xyz}形式的查询，例如，getProperty("${app.title}")，常用于@Value("${app.title}")注入；
 * 3. 带默认值的，以${abc.xyz:defaultValue}形式的查询，例如，getProperty("${app.title:Summer}")，常用于@Value("${app.title:Summer}")注入。
 * 
 * 不支持的：
 * 1. 组合查询，例如：jdbc.url=jdbc:mysql//${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME}
 * 2. #{...}表达式，例如：#{appBean.version() + 1}
 */
public class PropertyResolver {

    Map<String, String> properties = new HashMap<>();

    // 存储Class -> Function
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        // 存入环境变量
        this.properties.putAll(System.getenv());
        // 存入Properties
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }

        // String类型:
        converters.put(String.class, s -> s);
        // boolean类型:
        converters.put(boolean.class, s -> Boolean.parseBoolean(s));
        converters.put(Boolean.class, s -> Boolean.valueOf(s));
        // int类型:
        converters.put(int.class, s -> Integer.parseInt(s));
        converters.put(Integer.class, s -> Integer.valueOf(s));
        // 其他基本类型...
        // Date/Time类型:
        converters.put(LocalDate.class, s -> LocalDate.parse(s));
        converters.put(LocalTime.class, s -> LocalTime.parse(s));
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s));
        converters.put(ZonedDateTime.class, s -> ZonedDateTime.parse(s));
        converters.put(Duration.class, s -> Duration.parse(s));
        converters.put(ZoneId.class, s -> ZoneId.of(s));
    }

    /**
     * 获取配置的函数，按照是否有默认值来分可以分为两类
     * 
     * @param key
     * @return
     */
    @Nullable
    public String getProperty(String key) {
        // 表达式查询(${abc.xyz:defaultValue})
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            // 递归调用以支持嵌套的表达式，如${app.title:${APP_NAME:Summer}}
            if (keyExpr.defaultValue() != null) {
                // 带有默认值
                return getProperty(keyExpr.key(), keyExpr.defaultValue());
            } else {
                // 不带默认值
                return getRequiredProperty(keyExpr.key());
            }
        }

        // 普通key查询
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return value;
    }

    /**
     * 获取配置，如果配置不存在，则返回defaultValue
     * 
     * @param key
     * @param defaultValue
     * @return
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    /**
     * 获取配置，并且指定类型
     * 
     * @param <T>
     * @param key
     * @param clazz
     * @return
     */
    @Nullable
    public <T> T getProperty(String key, Class<T> clazz) {
        String value = getProperty(key);
        // 转换为指定类型
        return value == null ? null : convert(clazz, value);
    }

    /**
     * 获取配置，如果配置不存在，则返回defaultValue，同时指定返回的类型
     * 
     * @param <T>
     * @param key
     * @param targetType
     * @param defaultValue
     * @return
     */
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    String parseValue(String value) {
        PropertyExpr valueExpr = parsePropertyExpr(value);
        if (valueExpr == null) {
            return value;
        }
        if (valueExpr.defaultValue() != null) {
            return getProperty(valueExpr.key(), valueExpr.defaultValue());
        } else {
            return getRequiredProperty(valueExpr.key());
        }
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        // TODO: 我觉得这里很让人疑惑，要么就不能返回null，要么就与@Nullable一致
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    /*
     * 转换到指定Class类型
     */
    @SuppressWarnings("unchecked")
    <T> T convert(Class<T> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupported value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }

    /*
     * 对${...}表达式进行解析
     */
    PropertyExpr parsePropertyExpr(String expr) {
        if (expr.startsWith("${") && expr.endsWith("}")) {
            // 是否存在defaultValue
            int idx = expr.indexOf(":");
            if (idx == -1) {
                // 没有defaultValue(${key})
                String key = expr.substring(2, expr.length() - 1);
                return new PropertyExpr(key, null);
            } else {
                // 有defaultValue(${key:defaultValue})
                String key = expr.substring(2, idx);
                String defaultValue = expr.substring(idx + 1, expr.length() - 1);
                return new PropertyExpr(key, defaultValue);
            }
        }
        return null;
    }
}

/*
 * 保存表达式的解析结果
 */
record PropertyExpr(String key, String defaultValue) {

}
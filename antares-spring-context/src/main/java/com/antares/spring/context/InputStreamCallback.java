package com.antares.spring.context;

import java.io.IOException;
import java.io.InputStream;

/**
 * @FunctionalInterface: 是Java8的新特性，用于标识函数式接口
 * 一个接口被称为函数式接口，意味着它只包含一个抽象方法，能够被用作lambda表达式或方法引用的目标
 */
@FunctionalInterface
public interface InputStreamCallback<T> {
    T doWithInputStream(InputStream stream) throws IOException;
}

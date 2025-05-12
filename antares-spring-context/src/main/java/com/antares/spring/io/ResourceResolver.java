package com.antares.spring.io;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceResolver {
    /*
     * 这行代码创建了一个 Logger 对象。
     * LoggerFactory.getLogger(getClass())
     * 会根据当前类（ResourceResolver）的名字来创建与该类关联的日志记录器（Logger）。
     * 这样，生成的日志消息会带有该类的名字，方便我们定位日志来源。
     */
    Logger logger = LoggerFactory.getLogger(getClass());

    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    /*
     * <R> 表示方法使用了一个泛型类型。这个泛型类型将允许方法在调用时指定返回类型。
     * List<R> 是返回类型
     * Function<Resource, R> 是 Java 中的函数式接口，它接受一个 Resource 类型的输入，并返回一个类型为 R 的输出。
     */
    public <R> List<R> scan(Function<Resource, R> mapper) {
        String basePackagePath = basePackage.replace(".", "/");

        try {
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, collector, mapper);
            return collector;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    <R> void scan0(String basePackagePath, List<R> collector, Function<Resource, R> mapper)
            throws IOException, URISyntaxException {
        logger.atDebug().log("scan path: {}", basePackagePath);
        Enumeration<URL> en = getContextClassLoader().getResources(basePackagePath);
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            //TODO 删除前导的文件分隔符是否是必要的？因为uriToString后前导的应该是file:或jar:
            String uriStr = removeLeadingSlash(uriToString(uri));
            //去掉包名
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if (uriBaseStr.startsWith("file:")) {
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriBaseStr.startsWith("jar:")) {
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }
    }

    <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper)
            throws IOException, URISyntaxException {
        String baseDir = removeTrailingSlash(base);
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:" + path, name);
            }
            logger.atDebug().log("found resource: {}", res);
            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    /*
     * 获取当前线程的上下文类加载器，如果当前线程的上下文类加载器为空，则返回当前类的类加载器
     */
    ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = this.getClass().getClassLoader();
        }
        return cl;
    }

    /*
     * 将 URI 对象转换为字符串表示 (编码过的URI，例如空格会被转换成 %20)
     * 并使用 UTF-8 解码 URL 编码的部分，返回一个解码后的字符串。
     */
    String uriToString(URI uri) {
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8);
    }

    /*
     * 将指向 JAR 文件的 URI对象转换为文件系统中的一个 Path 对象
     * 并根据传入的 basePackagePath 获取该 JAR 文件内部的路径
     */
    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }

    /*
     * 删除前导的文件分隔符
     */
    String removeLeadingSlash(String s) {
        if (s.startsWith(File.separator)) {
            s = s.substring(1);
        }
        return s;
    }

    /*
     * 作用同上
     */
    String removeTrailingSlash(String s) {
        if (s.endsWith(File.separator)) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}

### 3. 相关工具包的知识

#### 3.1 `slf4j`

`slf4j`是一个Java日志框架的**抽象层**，它提供了一套统一的日志API，但日志的实际输出由底层绑定的日志实现框架决定（如Logback、Log4j等）。

##### 3.1.1 配置slf4j的步骤（以Logback为例）

###### 添加pom依赖：
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.13</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.14</version>
</dependency>
```

###### 添加Logback配置文件：`logback.xml`（放在`resources/`）
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%d{HH:mm:ss}] [%level] [%logger{36}] - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 设置全局日志级别 -->
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

    <!-- 为特定包设置更详细的日志 -->
    <logger name="com.example" level="DEBUG"/>
</configuration>
```

##### 3.1.2 使用示例
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogExample {
    private static final Logger logger = LoggerFactory.getLogger(LogExample.class);

    public static void main(String[] args) {
        logger.atTrace().log("This is a TRACE message");
        logger.atDebug().log("This is a DEBUG message");
        logger.atInfo().log("This is an INFO message");
        logger.atWarn().log("This is a WARN message");
        logger.atError().log("This is an ERROR message");
    }
}
```
`logger.atDebug().log(...)` 只有在 `logger` 的级别是 `DEBUG` 或更细（`TRACE`） 时才会生效，其余同理
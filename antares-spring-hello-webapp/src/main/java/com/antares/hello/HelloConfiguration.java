package com.antares.hello;

import com.antares.spring.annotation.ComponentScan;
import com.antares.spring.annotation.Configuration;
import com.antares.spring.annotation.Import;
import com.antares.spring.jdbc.JdbcConfiguration;
import com.antares.spring.web.WebMvcConfiguration;

@ComponentScan
@Configuration
// 被@Import的类自己也需要添加@Configuration注解，否则将不再满足其中@Bean的单例性
@Import({ JdbcConfiguration.class, WebMvcConfiguration.class })
public class HelloConfiguration {

}

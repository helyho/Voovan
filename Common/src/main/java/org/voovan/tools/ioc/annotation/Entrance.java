package org.voovan.tools.ioc.annotation;

import org.voovan.tools.ioc.IOCUtils;
import org.voovan.tools.reflect.annotation.Alias;

import java.lang.annotation.*;

/**
 * 入口注解, 需配合 @Bean 一起使用, 否则无效
 *   保证容器启动时优先初始化被 @Entrance 注解的类
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Entrance {
    @Alias("index")
    int value() default 0;
    @Alias("value")
    int index() default 0;
}

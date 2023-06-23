package org.voovan.tools.ioc.annotation;

import org.voovan.tools.reflect.annotation.Alias;

import java.lang.annotation.*;

/**
 * 需要优先初始化的bean, 需配合 @Bean 一起使用, 否则无效
 *   保证容器启动时优先初始化被 @Priority 注解的类
 *   可通过 priority 属性确定优先级, 数字越大优先级越高
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Priority {
    @Alias("priority")
    int value() default 0;
    @Alias("value")
    int priority() default 0;
}

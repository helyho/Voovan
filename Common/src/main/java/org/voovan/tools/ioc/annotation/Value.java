package org.voovan.tools.ioc.annotation;

import org.voovan.tools.reflect.annotation.Alias;

import java.lang.annotation.*;

/**
 * 自动注入注解
 * 使用在方法或构造函数上: 自动按方法参数的类型或者类型上的@Autowired的方式进行注入
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    //配置参数的路径
    @Alias("anchor")
    String value() default "";
    //如果值为 null 则取其默认值
    @Alias("value")
    String anchor() default "";

    boolean required()  default true;
    //todo:
}

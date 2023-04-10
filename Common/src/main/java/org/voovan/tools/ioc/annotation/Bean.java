package org.voovan.tools.ioc.annotation;

import org.voovan.tools.ioc.IOCUtils;
import org.voovan.tools.reflect.annotation.Alias;

import java.lang.annotation.*;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: ht tps://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    //配置参数的路径
    @Alias("name")
    String value() default "";
    //如果值为 null 则取其默认值
    @Alias("value")
    String name() default "";

    String scope() default IOCUtils.DEFAULT_SCOPE;

    //使用 class 的名称作为 bean 的名称
    boolean useClassName()  default true;

    boolean singleton()  default true;

    boolean lazy() default false;
}

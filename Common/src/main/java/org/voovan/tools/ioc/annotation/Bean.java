package org.voovan.tools.ioc.annotation;

import org.voovan.tools.ioc.Utils;
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

    String scope() default Utils.DEFAULT_SCOPE;

    boolean singleton()  default true;

    boolean lazy() default false;

    //初始化方法, 必须是无参数方法
    String init() default "";

    //对象销毁方法, 必须是无参数方法
    String destory() default "";
}

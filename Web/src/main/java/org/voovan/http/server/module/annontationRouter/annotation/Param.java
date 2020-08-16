package org.voovan.http.server.module.annontationRouter.annotation;

import java.lang.annotation.*;

/**
 * 注解路由 Param
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {
    String value() default "";
    boolean isRequire() default true;
    String defaultVal() default "";

    //-------------- API 文档相关属性 --------------
    /**
     * 参数说明
     * @return  参数说明
     */
    String description() default "";
}

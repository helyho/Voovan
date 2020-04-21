package org.voovan.http.server.module.annontationRouter.annotation;

import java.lang.annotation.*;

/**
 * Head 的注解路由
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Head {
    String value() default "";
    boolean isRequire() default true;
}

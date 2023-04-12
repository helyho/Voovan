package org.voovan.http.server.module.annontationRouter.annotation;

import java.lang.annotation.*;

/**
 * 注解路由 Session
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Session {
    String value() default "";
    boolean isRequire() default true;
    String defaultVal() default "";

    /**
     * 是否隐藏参数。
     *
     * @return true: 隐藏不可展示, false: 可展示
     */
    boolean hide() default false;
}

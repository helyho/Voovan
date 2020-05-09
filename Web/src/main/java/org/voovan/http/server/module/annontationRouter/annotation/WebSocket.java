package org.voovan.http.server.module.annontationRouter.annotation;

import java.lang.annotation.*;

/**
 * WebSocket 路由注解类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WebSocket {
    /**
     * 请求的 URL
     * @return 请求路径
     */
    String path() default "";

    /**
     *
     * @return 默认路径
     */
    String value() default "";
}

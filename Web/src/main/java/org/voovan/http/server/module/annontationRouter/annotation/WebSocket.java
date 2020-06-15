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

    //-------------- API 文档相关属性 --------------
    /**
     * 标签
     * @return 标签数组
     */
    String[] tags() default {};

    /**
     * 接口功能简述
     * @return 接口功能简述
     */
    String summary() default "";

    /**
     * 接口功能详细说明
     * @return 接口功能详细说明
     */
    String description() default "";
}

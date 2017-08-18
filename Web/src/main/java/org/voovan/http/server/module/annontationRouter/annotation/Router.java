package org.voovan.http.server.module.annontationRouter.annotation;

import java.lang.annotation.*;

/**
 * 请求注解类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Router {
     /**
      * 请求的 URL
       * @return 请求的 URL
      */
    String path() default "";

    /**
     * 请求的 URL
     * @return 请求的 URL
     */
    String value() default "";

     /**
      * 请求的方法
      * @return 请求的方法
      */
    String method() default "GET";

    /**
     * 定义路由类是否采用单例模式
     * 在类上则标识类会被提前实例化, 在路由方法上,则使用提前实例化的类进行调用
     * 在方法上无效
     *
     * @return true: 单例模式, false: 非单例模式
     */
    boolean singleton() default false;
}

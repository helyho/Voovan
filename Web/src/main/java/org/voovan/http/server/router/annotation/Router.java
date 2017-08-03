package org.voovan.http.server.router.annotation;

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
       * @return
      */
    String path() default "";

    /**
     *
     * @return
     */
    String value() default "";

     /**
      * 请求的方法
      * @return
      */
    String method() default "GET";
}

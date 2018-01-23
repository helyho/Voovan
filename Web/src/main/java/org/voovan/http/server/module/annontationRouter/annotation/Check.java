package org.voovan.http.server.module.annontationRouter.annotation;

import java.lang.annotation.*;

/**
 * 请求参数检查
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */

@Repeatable(value = Checks.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Check {

    /**
     * 信息
     * @return
     */
    String response() default "null";

    /**
     * 方法
     * 形式类似 classpath#method
     * @return
     */
    String responseMethod() default "null";

    /**
     * 数据值
     * @return
     */
    String value() default "null";

    /**
     * 方法
     * 形式类似 classpath#method
     * @return
     */
    String valueMethod() default "null";



    /**
     * 参数名称
     * @return
     */
    String name() default "null";

    /**
     * 数据来源
     * @return
     */
    Source Source() default Source.REQ_PARAM;

}



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
     * @return 信息
     */
    String response() default "null";

    /**
     * 响应方法
     * 形式类似 classpath#method
     * @return 响应方法
     */
    String responseMethod() default "null";

    /**
     * 数据值
     * @return 数据值
     */
    String value() default "null";

    /**
     * 参数值处理方法
     * 形式类似 classpath#method
     * @return 参数值处理方法
     */
    String valueMethod() default "null";

    /**
     * 参数名称
     * @return 参数名称
     */
    String name() default "null";

    /**
     * 数据来源
     * @return 数据来源
     */
    Source Source() default Source.REQ_PARAM;

}



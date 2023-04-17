package org.voovan.http.server.module.annontationRouter.annotation;

import org.voovan.http.HttpContentType;
import org.voovan.tools.reflect.annotation.Alias;

import java.lang.annotation.*;

/**
 * 路由注解类
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Repeatable(value = Routers.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Router {
    /**
     * 请求的 URL
     * @return 请求路径
     */
    @Alias("value")
    String path() default "";

    /**
     *
     * @return 默认路径
     */
    @Alias("path")
    String value() default "";

    /**
     * 请求的方法
     * @return 请求的方法
     */
    String[] method() default "GET";


    /**
     * Content-Type 配置
     * @return HttpContentType 枚举
     */
    HttpContentType contentType() default HttpContentType.PLAIN;

    /**
     * 定义路由类是否采用单例模式
     * 在类上则标识类会被提前实例化, 在路由方法上,则使用提前实例化的类进行调用
     * 在方法上无效
     * @return true: 单例模式, false: 非单例模式
     */
    boolean singleton() default true;

    boolean async() default false;

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

    /**
     * 接口是否已经被废弃，默认是false。
     *
     * @return true: 废弃, false: 可用
     */
    boolean deprecated() default false;

    /**
     * 是否隐藏接口。
     *
     * @return true: 隐藏不可展示, false: 可展示
     */
    boolean hide() default false;

}

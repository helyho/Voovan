package org.voovan.http.server.module.annontationRouter.swagger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Api 附加参数注解
 * 
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Repeatable(value = ApiParams.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiParam {
    String value() default "";

    /**
     * 请求的方法
     * @return 请求的方法
     */
    Class clazz() default String.class;

    /**
     * 参数位置
     * [path, header, cookie]
     */
    String position() default "path";
    
    boolean isRequire() default true;
    String defaultVal() default "";

    //-------------- API 文档相关属性 --------------
    /**
     * 参数说明
     * @return  参数说明
     */
    String description() default "";

    /**
     * 样例数据
     * @return 样例数据
     */
    String example() default "";
}

package org.voovan.http.server.module.annontationRouter.annotation;

import java.lang.annotation.*;

/**
 * 注解路由 BodyParam
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BodyParam {

    String value() default "";
    boolean isRequire() default true;
    String defaultVal() default "";

    //当参数为 Map 类型时指定他的 key 的类型
    Class keyType() default Object.class;
    //当参数为 Map 类型时指定他的 value 的类型
    Class valueType() default Object.class;


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

    /**
     * 是否隐藏参数。
     *
     * @return true: 隐藏不可展示, false: 可展示
     */
    boolean hide() default false;
}

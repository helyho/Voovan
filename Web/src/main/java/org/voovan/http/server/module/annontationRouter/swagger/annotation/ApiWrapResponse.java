package org.voovan.http.server.module.annontationRouter.swagger.annotation;

import java.lang.annotation.*;

/**
 * 用来包裹响应
 *      有的时候会使用 Filter 来变更响应类型, 通过这个注解可以在 swagger 中变更响应类型的描述
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiWrapResponse {
    // 响应变更的目标类型
    Class value();

    //真实响应在响应变更的新类型中的字段名称
    String field() default "";

}

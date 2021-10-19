package org.voovan.http.server.module.annontationRouter.swagger.annotation;

import java.lang.annotation.*;

/**
 * 重复路由注解
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */


@Target({ElementType.TYPE,  ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiGenerics {
    ApiGeneric[] value();
}

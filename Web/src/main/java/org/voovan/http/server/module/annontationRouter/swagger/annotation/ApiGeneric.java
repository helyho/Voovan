package org.voovan.http.server.module.annontationRouter.swagger.annotation;

import java.lang.annotation.*;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Repeatable(value = ApiGenerics.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiGeneric {
    String code() default "200";
    String description() default "successful";

    ApiGenericType genericType() default ApiGenericType.ALL;
    String genericName() default "";
    String genericProperty() default "";
    Class generic();
}

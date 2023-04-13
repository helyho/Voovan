package org.voovan.http.server.module.annontationRouter.swagger.annotation;

import java.lang.annotation.*;

/**
 * Class name
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
    Class value();

    String field() default "";

}

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


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Checks {
    Check[] value();
}

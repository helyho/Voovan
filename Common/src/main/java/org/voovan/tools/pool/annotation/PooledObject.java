package org.voovan.tools.pool.annotation;

import java.lang.annotation.*;

/**
 * 池化对象的注解路由
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface PooledObject {
}

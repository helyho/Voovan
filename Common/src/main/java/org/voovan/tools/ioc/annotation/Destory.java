package org.voovan.tools.ioc.annotation;

import java.lang.annotation.*;

/**
 * 对象销毁方法
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Destory {
}

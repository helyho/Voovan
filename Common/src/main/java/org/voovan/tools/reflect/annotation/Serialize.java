package org.voovan.tools.reflect.annotation;

import java.lang.annotation.*;

/**
 * getMapFromObject 讲不处理被注解的字段
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Serialize {
    String value() default "";
}

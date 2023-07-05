package org.voovan.tools.reflect.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * getMapFromObject 将不处理被注解的字段
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
public @interface Unserialization {
    
    /**
     * 反序列化的别名
     *  
     */
    @Alias("alias")
    String value() default "";

    @Alias("value")
    String alias() default "";
}

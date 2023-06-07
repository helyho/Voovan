package org.voovan.tools.reflect.annotation;

import org.voovan.tools.reflect.exclude.Exclude;
import org.voovan.tools.reflect.convert.Convert;

import java.lang.annotation.*;

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
public @interface Serialization {
    
    /* 序列化的别名 */
    @Alias("toAlias")
    String value() default "";

    @Alias("value")
    String toAlias() default "";

    Class<? extends Convert> convert() default Convert.class;
    Class<? extends Exclude> exclude() default Exclude.class;


    /* 序列化的别名 */
    String fromAlias() default "";
}

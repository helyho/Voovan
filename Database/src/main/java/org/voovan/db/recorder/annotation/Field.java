package org.voovan.db.recorder.annotation;

import java.lang.annotation.*;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Field {
    String name() default "";
    String value() default "";
    int lowerCase() default 0;
    int upperCase() default 0;
}

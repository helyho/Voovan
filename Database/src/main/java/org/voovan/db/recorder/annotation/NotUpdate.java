package org.voovan.db.recorder.annotation;

import java.lang.annotation.*;

/**
 * Recorder 对象的 NotUpdate 注解
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotUpdate {
    String value() default "ANY_VALUE";
}

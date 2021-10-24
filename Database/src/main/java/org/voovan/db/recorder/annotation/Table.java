package org.voovan.db.recorder.annotation;

import org.voovan.db.DataBaseType;

import java.lang.annotation.*;

/**
 * Recorder 对象的 Table 注解
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Table {
    String database() default "";
    String name() default "";
    String value() default "";
    DataBaseType databaseType() default DataBaseType.UNKNOW;
    boolean lowerCase() default false;
    boolean upperCase() default false;
    boolean upperCaseHead() default false;
    boolean camelToUnderline() default true;
}

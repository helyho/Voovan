package org.voovan.tools.cmd.annotation;

import java.lang.annotation.*;

/**
 * GNU style options
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Option {
    String name() default "";
    String longName() default "";
    String usage() default "";
    boolean required() default false;
}

package org.voovan.tools.cmd.annotation;

import java.lang.annotation.*;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Command {
    String description() default "";
    String usage() default "";
    String version() default "";
    String copyright() default "";
    String author() default "";
    String contact() default "";
    String licence() default "";
}

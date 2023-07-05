package org.voovan.tools.reflect.annotation;

import java.lang.annotation.*;

/**
 * 注解属性的别名
 * 在使用 TReflect.getAnnotationValue 时同步几个属性的值
 * 被 @Alias 注解的属性如果值为 default定义的值, 则使用@Alias注解中描述的属性的值代替
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Alias {
    String value();
}

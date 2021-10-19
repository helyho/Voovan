package org.voovan.http.server.module.annontationRouter.swagger.annotation;

import java.lang.annotation.*;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Repeatable(value = ApiGenerics.class)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**
 * 由于是 json 类型, 如果是范型对象是 Map, 那么这里标记的是 value 的类型, key 固定为 string
 */
public @interface ApiGeneric {
    String param() default "";
    String property() default "";
    Class[] clazz();
}

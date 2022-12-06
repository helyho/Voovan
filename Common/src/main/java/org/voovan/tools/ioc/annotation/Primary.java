package org.voovan.tools.ioc.annotation;

import java.lang.annotation.*;

/**
 * 标注按类型获取对象时的首选对象, 需要同 @Bean, @Value 同时使用
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {
}

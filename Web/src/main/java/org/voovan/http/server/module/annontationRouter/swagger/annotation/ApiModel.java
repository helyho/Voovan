package org.voovan.http.server.module.annontationRouter.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiModel {
    /**
     * 实体类说明, 作为响应时的描述
     * @return 实体类说明
     */
    String value() default "";

    /**
     * 实体需要隐藏的属性, 用于继承在不同场景下隐藏不同的属性
     * @return 需要隐藏的属性
     */
    String[] hiddenProperty() default "";
}
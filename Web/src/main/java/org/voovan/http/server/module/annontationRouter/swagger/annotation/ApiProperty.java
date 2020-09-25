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
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiProperty {
    /**
     * 实体属性说明
     * @return 实体属性说明
     */
    String value() default "";


    boolean isRequire() default false;

    /**
     * 样例数据
     * @return 样例数据
     */
    String example() default "";

    /**
     * 样例数据
     * @return 样例数据
     */
    boolean hidden() default false;
}

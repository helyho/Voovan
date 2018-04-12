package org.voovan.tools.aop.annotation;

import java.lang.annotation.*;

/**
 * 切面点描述类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface After {
    /**
     * 切入点 java.lang.String org.voovan.test.service.web@IndexMethod(java.lang.String, java.lang.String)
     * @return 切入点
     */
    String value() default "* *(..)";
}

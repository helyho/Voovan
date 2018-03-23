package org.voovan.tools.aop.annotation;

import java.lang.annotation.*;

/**
 * 切面扫描点类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Aop {
}

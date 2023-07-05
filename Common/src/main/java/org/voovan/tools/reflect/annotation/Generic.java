package org.voovan.tools.reflect.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 范型描述注解
 *  在使用TReflect.getObjectFromMap时有些内部潜逃对象的范型会丢失,使用这个注解描述则才反序列化的过程使用这里描述的范型进行处理 
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Generic {
      Class[] generics();
}

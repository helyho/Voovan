package org.voovan.tools.json.annotation;

import java.lang.annotation.*;

/**
 * JSON 字段不生成至 JSON 字符串
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotJSON {
}

package org.voovan.tools.reflect;

import org.voovan.tools.json.JSON;

import java.text.ParseException;
import java.util.Map;

/**
 * 实现这个接口, 自定义转换 Map 的方法, 来提高性能
 * 自定义 Object 转 Map 的逻辑
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface ToMap<T> {
    default Map<String, Object> toMap() throws ReflectiveOperationException {
        return TReflect.getMapfromObject(this);
    }
}

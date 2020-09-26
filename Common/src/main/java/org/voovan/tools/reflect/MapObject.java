package org.voovan.tools.reflect;

import org.voovan.tools.json.JSON;

import java.text.ParseException;
import java.util.Map;

/**
 * 自定义 Object -> Map 的数据
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface MapObject<T> {
    default Map<String, Object> toMap() throws ReflectiveOperationException {
        return TReflect.getMapfromObject(this);
    }
}

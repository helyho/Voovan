package org.voovan.tools.reflect.convert;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化数据类型转换
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface Convert<P, T> {
    public static Map<Class<? extends Convert>, Convert> CONVERT_CACHE = new ConcurrentHashMap<Class<? extends Convert>, Convert>();

    public static Convert getConvert(Class<Convert> clazz) {
        Convert convert = CONVERT_CACHE.get(clazz);
        if(convert == null) {
            synchronized (CONVERT_CACHE) {
                convert = CONVERT_CACHE.get(clazz);
                if(convert == null) {
                    try {
                        convert = TReflect.newInstance(clazz);
                        CONVERT_CACHE.put(clazz, convert);
                    } catch (ReflectiveOperationException e) {
                        Logger.errorf("Create convert {} failed", e, TReflect.getClassName(clazz));
                    }
                }
            }
        }

        return convert;
    }

    public T convert(P parameter);
}

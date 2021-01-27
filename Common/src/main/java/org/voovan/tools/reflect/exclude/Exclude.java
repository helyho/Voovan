package org.voovan.tools.reflect.exclude;

import javafx.scene.Parent;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据排除器
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface Exclude<P> {
    public static Map<Class<? extends Exclude>, Exclude> CHECK_CACHE = new ConcurrentHashMap<Class<? extends Exclude>, Exclude>();

    public static Exclude getExclude(Class<? extends Exclude> clazz) {
        if(clazz == null) {
            return null;
        }

        Exclude exclude = CHECK_CACHE.get(clazz);
        if(exclude == null) {
            synchronized (CHECK_CACHE) {
                exclude = CHECK_CACHE.get(clazz);
                if(exclude == null) {
                    try {
                        exclude = TReflect.newInstance(clazz);
                        CHECK_CACHE.put(clazz, exclude);
                    } catch (ReflectiveOperationException e) {
                        Logger.errorf("Create convert {} failed", e, TReflect.getClassName(clazz));
                    }
                }
            }
        }

        return exclude;
    }

    public boolean check(String name, P parameter);
}

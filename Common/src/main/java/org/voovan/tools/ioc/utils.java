package org.voovan.tools.ioc;

import org.voovan.tools.TEnv;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class utils {
    public static String classKey(Class clazz) {
        return TEnv.shortClassName(clazz.getName(), "");
    }
}

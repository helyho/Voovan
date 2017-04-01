package org.voovan.tools;

import org.voovan.tools.log.Logger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class TUnsafe {

    private static Unsafe unsafe;

    public static Unsafe getUnsafe() {
        if(unsafe==null) {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                unsafe = (Unsafe) field.get(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return unsafe;
    }
}

package org.voovan.test;

import org.voovan.tools.TByte;
import org.voovan.tools.TDateTime;
import org.voovan.tools.UniqueId;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.serialize.TSerialize;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Other {
    private static int apple = 10;
    private int orange = 10;

    public static void main(String[] args) throws Exception {
        final UniqueId uniqueId = new UniqueId(200);
        System.out.println("1970 " + uniqueId.getNumber(TDateTime.parse("1970-1-1 00:00:00").getTime()));
        System.out.println("1980 " + uniqueId.getNumber(TDateTime.parse("1980-1-1 00:00:00").getTime()));
        System.out.println("1990 " + uniqueId.getNumber(TDateTime.parse("1990-1-1 00:00:00").getTime()));
        System.out.println("2000 " + uniqueId.getNumber(TDateTime.parse("2000-1-1 00:00:00").getTime()));
        System.out.println("2010 " + uniqueId.getNumber(TDateTime.parse("2010-1-1 00:00:00").getTime()));
        System.out.println("2020 " + uniqueId.getNumber(TDateTime.parse("2020-1-1 00:00:00").getTime()));
        System.out.println("2030 " + uniqueId.getNumber(TDateTime.parse("2030-1-1 00:00:00").getTime()));
        System.out.println("2040 " + uniqueId.getNumber(TDateTime.parse("2040-1-1 00:00:00").getTime()));
        System.out.println(6617806405632409601L >> 22);

    }
}

package org.voovan.tools.reflect.convert;

import org.voovan.tools.TDateTime;

import java.util.Date;

/**
 * Date 形式的时间转换成时间戳
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ToTimeStamp implements Convert<Date, Long> {

    @Override
    public Long convert(String name, Date parameter) {
        if (parameter == null) {
            return null;
        }

        return parameter.getTime();
    }
}
package org.voovan.tools.reflect.convert;

import org.voovan.tools.TDateTime;

import java.math.BigDecimal;

/**
 * 时间戳转换成字符串形式的时间格式
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ToDateString implements Convert<Long, String> {

    @Override
    public String convert(String name, Long parameter) {
        if(parameter == null) {
            return null;
        }
        return TDateTime.timestamp(parameter);
    }
}

package org.voovan.tools.reflect.convert;

import java.math.BigDecimal;

/**
 * 序列化数据转字符串
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class StringConvert<T, String> implements Convert {

    @Override
    public Object convert(Object parameter) {
        if(parameter instanceof BigDecimal) {
            return ((BigDecimal)parameter).stripTrailingZeros().toPlainString();
        }

        return parameter.toString();
    }
}

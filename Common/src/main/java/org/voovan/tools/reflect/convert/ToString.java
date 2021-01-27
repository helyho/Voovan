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
public class ToString<T> implements Convert<T, String> {

    @Override
    public String convert(String name, Object parameter) {
        if(parameter == null) {
            return null;
        }

        if(parameter instanceof BigDecimal) {
            return ((BigDecimal)parameter).stripTrailingZeros().toPlainString();
        }

        return parameter.toString();
    }
}

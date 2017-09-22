package org.voovan.tools.json;

import org.voovan.tools.TDateTime;
import org.voovan.tools.TString;
import org.voovan.tools.reflect.TReflect;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JSON打包类
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSONEncode {
    /**
     * 分析自定义对象为JSON字符串
     *
     * @param object 自定义对象
     * @return JSON字符串
     * @throws ReflectiveOperationException
     */
    private static String complexObject(Object object) throws ReflectiveOperationException {
        return mapObject(TReflect.getMapfromObject(object));
    }

    /**
     * 分析Map对象为JSON字符串
     *
     * @param mapObject map对象
     * @return JSON字符串
     * @throws ReflectiveOperationException
     */
    private static String mapObject(Map<?, ?> mapObject) throws ReflectiveOperationException {
        String mapString = "{";
        StringBuilder ContentStringBuilder = new StringBuilder();
        String ContentString = null;

        Object[] keys = mapObject.keySet().toArray();

        for (Object mapkey : keys) {
            Object key = fromObject(mapkey);
            String Value = fromObject(mapObject.get(mapkey));
            ContentStringBuilder.append(key);
            ContentStringBuilder.append(":");
            ContentStringBuilder.append(Value);
            ContentStringBuilder.append(",");
        }

        ContentString = ContentStringBuilder.toString();
        if (!ContentString.trim().isEmpty()){
            ContentString = ContentString.substring(0, ContentString.length() - 1);
        }

        mapString = mapString + ContentString + "}";

        return mapString;
    }

    /**
     * 分析Collection对象为JSON字符串
     *
     * @param listObject List对象
     * @return JSON字符串
     * @throws ReflectiveOperationException
     */
    private static String CollectionObject(List<Object> listObject) throws ReflectiveOperationException {
        return arrayObject(listObject.toArray());
    }

    /**
     * 分析Array对象为JSON字符串
     *
     * @param arrayObject Array对象
     * @throws ReflectiveOperationException
     * @return JSON字符串
     */
    private static String arrayObject(Object[] arrayObject) throws ReflectiveOperationException {
        String arrayString = "[";
        String ContentString = "";
        StringBuilder ContentStringBuilder = new StringBuilder();

        for (Object object : arrayObject) {
            String Value = fromObject(object);
            ContentStringBuilder.append(Value);
            ContentStringBuilder.append(",");
        }

        ContentString = ContentStringBuilder.toString();

        if (!ContentString.trim().isEmpty()) {
            ContentString = ContentString.substring(0, ContentString.length() - 1);
        }

        arrayString = arrayString + ContentString + "]";
        return arrayString;
    }

    /**
     * 将对象转换成JSON字符串
     *
     * @param object 要转换的对象
     * @return 类型:String 		对象对应的JSON字符串
     * @throws ReflectiveOperationException 反射异常
     */
    @SuppressWarnings("unchecked")
    public static String fromObject(Object object) throws ReflectiveOperationException {
        String value = null;

        if (object == null) {
            value = "null";
        } else if (object instanceof BigDecimal) {
            value = ((BigDecimal)object).toString();
        } else if (object instanceof Date) {
            value = "\"" + TDateTime.format(((Date)object), TDateTime.STANDER_DATETIME_TEMPLATE)+ "\"";;
        } else if (object instanceof Map) {
            Map<Object, Object> mapObject = (Map<Object, Object>) object;
            value = mapObject(mapObject);
        } else if (object instanceof Collection) {
            List<Object> listObject = (List<Object>)object;
            value = CollectionObject(listObject);
        } else if (object.getClass().isArray()) {
            Object[] arrayObject = (Object[])object;
            value = arrayObject(arrayObject);
        } else if (object instanceof Integer ||  object instanceof Float ||
                object instanceof Double || object instanceof Boolean ||
                object instanceof Long || object instanceof Short) {
            value = object.toString();
        } else if (TReflect.isBasicType(object.getClass())) {
            //这里这么做的目的是方便 js 中通过 eval 方法产生 js 对象
            String strValue = TString.convertEscapeChar(object.toString());
            value = "\"" + strValue + "\"";
        }  else {
            value = complexObject(object);
        }

        return value;
    }


}

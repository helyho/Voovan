package org.voovan.tools.json;

import org.voovan.Global;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.collection.IntKeyMap;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.reflect.convert.Convert;
import org.voovan.tools.reflect.exclude.Exclude;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    public static boolean JSON_HASH = TEnv.getSystemProperty("JsonHash", false);
    public final static IntKeyMap<String> JSON_ENCODE_CACHE = new IntKeyMap<String>(1024);

    //<对象类型, 转换器>
    public final static ConcurrentHashMap<Class, Class<? extends Convert>> JSON_CONVERT = new ConcurrentHashMap<Class, Class<? extends Convert>>();

    public static void addConvert(Class objClass, Class<? extends Convert> convertClazz) {
        JSON_CONVERT.put(objClass, convertClazz);
    }

    static {
        if(JSON_HASH) {
            Global.getHashWheelTimer().addTask(new HashWheelTask() {
                @Override
                public void run() {
                    JSON_ENCODE_CACHE.clear();
                }
            }, 1);
        }
    }

    /**
     * 分析自定义对象为JSON字符串
     *
     * @param object 自定义对象
     * @return JSON字符串
     * @throws ReflectiveOperationException
     */
    private static String complexObject(Object object, boolean allField) throws ReflectiveOperationException {
        return mapObject(TReflect.getMapfromObject(object, allField), allField);
    }

    /**
     * 分析Map对象为JSON字符串
     *
     * @param mapObject map对象b
     * @return JSON字符串
     * @throws ReflectiveOperationException
     */
    private static String mapObject(Map<?, ?> mapObject, boolean allField) throws ReflectiveOperationException {
        StringBuilder contentStringBuilder = new StringBuilder(Global.STR_LC_BRACES);

        for (Object mapkey : mapObject.keySet()) {
            String key = fromObject(mapkey, allField);
            Object originValue = mapObject.get(mapkey);

            String value = null;
            if(originValue!=null && !allField) {
                //查找转换器并转换,基于对象类型转换
                Convert convert = Convert.getConvert(JSON_CONVERT.get(originValue.getClass()));
                if (convert != null) {
                    value = fromObject(convert.convert(key, originValue), true);
                } else {
                    value = fromObject(originValue, allField);
                }
            } else {
                value = fromObject(originValue, allField);
            }

            String wrapQuote = key.startsWith(Global.STR_QUOTE) && key.endsWith(Global.STR_QUOTE) ? Global.EMPTY_STRING : Global.STR_QUOTE;
            contentStringBuilder.append(wrapQuote);
            contentStringBuilder.append(key);
            contentStringBuilder.append(wrapQuote);
            contentStringBuilder.append(Global.STR_COLON);
            contentStringBuilder.append(value);
            contentStringBuilder.append(Global.STR_COMMA);
        }

        if (contentStringBuilder.length()>1){
            contentStringBuilder.setLength(contentStringBuilder.length() - 1);
        }

        contentStringBuilder.append(Global.STR_RC_BRACES);

        return contentStringBuilder.toString();
    }

    /**
     * 分析Array对象为JSON字符串
     *
     * @param arrayObject Array对象
     * @throws ReflectiveOperationException
     * @return JSON字符串
     */
    private static String arrayObject(Object[] arrayObject, boolean allField) throws ReflectiveOperationException {
        StringBuilder contentStringBuilder = new StringBuilder(Global.STR_LS_BRACES);

        for (Object object : arrayObject) {
            String value = null;
            if(object!=null && !allField) {
                //查找转换器并转换
                Convert convert = Convert.getConvert(JSON_CONVERT.get(object.getClass()));
                if (convert != null) {
                    value = fromObject(convert.convert(null, object), true);
                } else {
                    value = fromObject(object, allField);
                }
            } else {
                value = fromObject(object, allField);
            }

            contentStringBuilder.append(value);
            contentStringBuilder.append(Global.STR_COMMA);
        }

        if (contentStringBuilder.length()>1){
            contentStringBuilder.setLength(contentStringBuilder.length() - 1);
        }

        contentStringBuilder.append(Global.STR_RS_BRACES);
        return contentStringBuilder.toString();
    }

    /**
     * 分析Collection对象为JSON字符串
     *
     * @param collectionObject Collection 对象
     * @throws ReflectiveOperationException
     * @return JSON字符串
     */
    private static String CollectionObject(Collection collectionObject, boolean allField) throws ReflectiveOperationException {
        StringBuilder contentStringBuilder = new StringBuilder(Global.STR_LS_BRACES);

        for (Object object : collectionObject) {
            String value = null;
            if(object!=null && !allField) {
                //查找转换器并转换
                Convert convert = Convert.getConvert(JSON_CONVERT.get(object.getClass()));
                if (convert != null) {
                    value = fromObject(convert.convert(null, object), true);
                } else {
                    value = fromObject(object, allField);
                }
            } else {
                value = fromObject(object, allField);
            }
            contentStringBuilder.append(value);
            contentStringBuilder.append(Global.STR_COMMA);
        }

        if (contentStringBuilder.length()>1){
            contentStringBuilder.setLength(contentStringBuilder.length() - 1);
        }

        contentStringBuilder.append(Global.STR_RS_BRACES);
        return contentStringBuilder.toString();
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
        return fromObject(object, false);
    }

    /**
     * 将对象转换成JSON字符串
     *
     * @param object 要转换的对象
     * @param allField 是否处理所有属性
     * @return 类型:String 		对象对应的JSON字符串
     * @throws ReflectiveOperationException 反射异常
     */
    @SuppressWarnings("unchecked")
    public static String fromObject(Object object, boolean allField) throws ReflectiveOperationException {
        String value = null;

        if(object == null) {
            return "null";
        }

        int jsonHash = 0;
        if(JSON_HASH) {
            jsonHash = object.hashCode();
            jsonHash =  jsonHash + (allField ? 1 : 0);
            value = JSON_ENCODE_CACHE.get(jsonHash);

            if (value != null) {
                return value;
            }
        }

        Class clazz = object.getClass();

        if (object instanceof Class) {
            return "\"" + ((Class)object).getCanonicalName() + "\"";
        } else if (object instanceof BigDecimal) {
            value = ((BigDecimal) object).stripTrailingZeros().toPlainString();
        } else if (object instanceof Date) {
            value = "\"" + TDateTime.format(((Date)object), TDateTime.STANDER_DATETIME_TEMPLATE)+ "\"";;
        } else if (object instanceof Map) {
            Map<Object, Object> mapObject = (Map<Object, Object>) object;
            value = mapObject(mapObject, allField);
        } else if (object instanceof Collection) {
            Collection<Object> collectionObject = (Collection<Object>)object;
            value = CollectionObject(collectionObject, allField);
        } else if (clazz.isArray()) {
            Object[] arrayObject = (Object[])object;
            //如果是 java 基本类型, 则转换成对象数组
            if(clazz.getComponentType().isPrimitive()) {
                int length = Array.getLength(object);
                arrayObject = new Object[length];
                for(int i=0;i<length;i++){
                    arrayObject[i] = Array.get(object, i);
                }
            } else {
                arrayObject = (Object[])object;
            }
            value = arrayObject(arrayObject, allField);
        } else if (object instanceof Integer ||  object instanceof Float ||
                object instanceof Double || object instanceof Boolean ||
                object instanceof Long || object instanceof Short) {
            value = object.toString();
        } else if (object instanceof AtomicLong || object instanceof AtomicInteger ||
                object instanceof AtomicBoolean) {
            value = TReflect.invokeMethod(object, "get").toString();
        } else if (TReflect.isBasicType(object.getClass())) {
            //这里这么做的目的是方便 js 中通过 eval 方法产生 js 对象
            String strValue = object.toString();
            if(JSON.isConvertEscapeChar()) {
                strValue = TString.convertEscapeChar(object.toString());
            }
            value = "\"" + strValue + "\"";
        }  else {
            value = complexObject(object, allField);
        }

        if(JSON_HASH) {
            JSON_ENCODE_CACHE.put(jsonHash, value);
        }

        return value;
    }
}

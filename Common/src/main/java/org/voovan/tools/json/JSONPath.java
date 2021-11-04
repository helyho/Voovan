package org.voovan.tools.json;

import org.voovan.tools.TObject;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * JSON 使用路径解析的工具类
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class JSONPath {

    private Object parsedObj;

    public JSONPath(String jsonStr) {
        if(jsonStr.startsWith("http")) {
            try {
                URL url = new URL(jsonStr);
                Object object = url.getContent();
                jsonStr = new String(TStream.readAll((InputStream)object));
            } catch (Exception e) {
                Logger.error("Load JSONPath error: " + jsonStr);
            }
        }

        Object result = JSON.parse(jsonStr);
        if (result instanceof List) {
            parsedObj = (List)result;
        } else if (result instanceof Map) {
            parsedObj = (Map)result;
        }
    }

    public Object parse(String pathQry, Object parsedObj) {
        Object currentPathObject = parsedObj;
        String[] pathElems = pathQry.split("/");
        ArrayList result = new ArrayList();

        for (int m=0;m<pathElems.length;m++) {
            String pathElem = pathElems[m];

            if(currentPathObject == null) {
                return null;
            }

            pathElem = pathElem.trim();

            if (pathElem.isEmpty()) {
                continue;
            }

            //获取 list 索引位置
            if ( pathElem.indexOf("[") > -1 &&  pathElem.indexOf("]") > -1 ) {
                //多级数组支持
                String[] pathElemSegms = pathElem.trim().split("\\[");

                for(int i=0;i<pathElemSegms.length;i++ ){
                    String pathElemSegm = pathElemSegms[i];
                    if(pathElemSegm.isEmpty()){
                        continue;
                    }
                    if(pathElemSegm.endsWith("]")){
                        int index = Integer.parseInt(TString.removeSuffix(pathElemSegms[i]));
                        currentPathObject = ((List) currentPathObject).get(index);
                    }else {
                        currentPathObject = (List) ((Map) currentPathObject).get(pathElemSegms[i]);
                    }
                }
            }else{
                if(currentPathObject instanceof List) {
                    //遍历list中的元素
                    StringJoiner stringJoiner = new StringJoiner("/");
                    for(int k=m; k<pathElems.length;k++) {
                        stringJoiner.add(pathElems[k]);
                    }

                    List currentList = (List)currentPathObject;
                    List ret = new ArrayList();
                    for(Object listElem : currentList) {
                        ret.add(parse(stringJoiner.toString(), listElem));
                    }
                    return ret;
                }

                if(currentPathObject instanceof Map) {

                    currentPathObject = ((Map) currentPathObject).get(pathElem);
                }
            }
        }

        return currentPathObject;
    }

    /**
     * 获取JSONPath 对应的节点数据,忽略段大小写
     * @param pathQry JSONPath 路径
     * @return  节点的数据
     */
    public Object value(String pathQry) {
        return parse(pathQry, parsedObj);
    }


    /**
     * 获取JSONPath 对应的节点数据,默认忽略段大小写
     * @param pathQry JSONPath 路径
     * @param defaultValue 节点不存在时的默认值
     * @param <T>     范型
     * @return  节点的数据
     */
    public <T> T value(String pathQry, T defaultValue) {
        return (T)TObject.nullDefault(value(pathQry),defaultValue);
    }

    /**
     * 获取节点值并转换成相应的对象,默认忽略段大小写
     * @param pathQry  JSONPath 路径
     * @param clazz    对象的 class, Map:作为转换目标对象的类型, List 作为元素的类型
     * @param <T>      范型指代对象
     * @return  转换后的对象
     */
    public <T> T value(String pathQry, Class<T> clazz) {
        Object value = value(pathQry);

        if(value==null){
            return null;
        }

        Object obj = null;

        try {
            if(value instanceof Map) {
                obj = TReflect.getObjectFromMap(clazz, (Map<String, ?>) value, true);
            } else if(value instanceof List) {
                List objList = (List)value;
                obj = objList.stream().map(item-> {
                    try {
                        if(item instanceof Map){
                            return TReflect.getObjectFromMap(clazz, (Map<String, ?>) item, true);
                        } else {
                            return TString.toObject(item.toString(), clazz);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }).filter(item->item!=null).collect(Collectors.toList());
            } else if (TReflect.isSystemType(clazz)) {
                if(TReflect.isSuper(value.getClass(), clazz)) {
                    obj = value;
                } else if(TReflect.isBasicType(clazz)){
                    obj = TReflect.newInstance(clazz, value.toString());
                } else if (clazz.equals(BigDecimal.class)) {
                    obj = TReflect.newInstance(clazz, value.toString());
                }
            }
        } catch (Exception e) {
            Logger.error("Parse " + pathQry + "error", e);
        }

        return (T)obj;
    }

    /**
     * 获取节点值并转换成相应的对象,忽略段大小写
     * @param pathQry  JSONPath 路径
     * @param clazz    对象的 class
     * @param defaultValue 对象默认值
     * @param <T>      范型指代对象
     * @return  转换后的对象
     */
    public <T> T value(String pathQry, Class<T> clazz, T defaultValue) {
        return TObject.nullDefault(value(pathQry, clazz), defaultValue);
    }

    /**
     * 获取节点值并转换成相应的 Map 对象,忽略段大小写
     * @param <T>      范型指代对象
     * @param pathQry  JSONPath 路径
     * @param genericType 对象范型类型
     * @return  转换后的对象
     */
    public <T> T mapObject(String pathQry, Class[] genericType) {
        List<T> resultList = new ArrayList<T>();
        Map<String,?> mapValue = value(pathQry,Map.class);

        if(mapValue==null){
            return null;
        }

        try {
            return (T)TReflect.getObjectFromMap(Map.class, mapValue, genericType, false);
        } catch (Exception e) {
            Logger.error("Parse " + pathQry + "error", e);
        }

        return null;
    }

    /**
     * 获取节点值并转换成相应的 Map 对象,忽略段大小写
     * @param <T>      范型指代对象
     * @param pathQry  JSONPath 路径
     * @param genericType 对象范型类型
     * @param defaultValue 对象默认值
     * @return  转换后的对象
     */
    public <T> T mapObject(String pathQry, Class[] genericType, T defaultValue) {
        return TObject.nullDefault(mapObject(pathQry, genericType), defaultValue);
    }


    /**
     * 获取节点值并转换成相应的 List 对象,忽略段大小写
     * @param <T>      范型指代对象
     * @param pathQry  JSONPath 路径
     * @param elemClazz    List 元素对象的 class
     * @return  转换后的对象
     */
    public <T> List<T> listObject(String pathQry, Class<T> elemClazz) {
        return (List<T>) value(pathQry, elemClazz);
    }

    /**
     * 获取节点值并转换成相应的对象,忽略段大小写
     * @param <T>      范型指代对象
     * @param pathQry  JSONPath 路径
     * @param elemClazz    List 元素对象的 class
     * @param defaultValue 节点不存在时的默认值
     * @return  转换后的对象
     */
    public <T> List<T> listObject(String pathQry, Class<T> elemClazz, List<T> defaultValue) {
        List<T> result = listObject(pathQry,elemClazz);
        if(result==null){
            return defaultValue;
        }else {
            return result;
        }
    }


    /**
     * 将 JSON 中的对象中的一个节点自动 转换成 java 中的对象,忽略段大小写
     * @param pathQry        JSONPath 路径
     * @param keyFieldName   key 值在 java 对象中对应的字段
     * @param elemClazz      对象的 class
     * @param <T>            范型指代对象
     * @return  转换后的对象
     */
    public <T> List<T> mapToListObject(String pathQry, String keyFieldName, Class<?> elemClazz) {
        List<T> resultList = new ArrayList<T>();
        Map<String,?> mapValue = value(pathQry, Map.class);

        if(mapValue==null){
            return null;
        }

        Map map = null;
        for(Map.Entry<String,?> entry : mapValue.entrySet()){
            String key = entry.getKey();
            Object value = entry.getValue();

            if(TReflect.isSuper(elemClazz, Map.class)) {
                resultList.add((T)TObject.asMap(key, value));
            } else {
                if (value instanceof Map) {
                    map = (Map) value;
                } else {
                    map = TObject.asMap("", value);
                }
                map.put(keyFieldName, key);
                try {
                    T obj = (T) TReflect.getObjectFromMap(elemClazz, map, true);
                    resultList.add(obj);
                } catch (Exception e) {
                    Logger.error("Parse " + pathQry + "error", e);
                }
            }
        }

        return resultList;
    }

    /**
     * 将 JSON 中的对象中的一个节点自动 转换成 java 中的对象,忽略段大小写
     * @param pathQry        JSONPath 路径
     * @param keyFieldName   key 值在 java 对象中对应的字段
     * @param elemClazz      对象的 class
     * @param defaultValue   默认值
     * @param <T>            范型指代对象
     * @return  转换后的对象
     */
    public <T> List<T> mapToListObject(String pathQry,String keyFieldName,Class<?> elemClazz, List<T> defaultValue) {
        List<T> result = mapToListObject(pathQry,keyFieldName,elemClazz);
        if(result==null){
            return defaultValue;
        }else {
            return result;
        }
    }

    /**
     * 构造默认的对象
     * @param jsonStr JSONPath 路径
     * @return  转换后的对象
     */
    public static JSONPath newInstance(String jsonStr){
        return new JSONPath(jsonStr);
    }

}

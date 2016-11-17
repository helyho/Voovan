package org.voovan.tools.json;

import org.voovan.tools.TObject;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.TString;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 类文字命名
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
        Object result = JSON.parse(jsonStr);
        if (result instanceof List) {
            parsedObj = TObject.cast(result);
        } else if (result instanceof Map) {
            parsedObj = TObject.cast(result);
        }
    }

    /**
     * 获取JSONPath 对应的节点数据
     * @param pathQry JSONPath 路径
     * @return  节点的数据
     * @throws ReflectiveOperationException 反射操作异常
     */
    public Object value(String pathQry) throws ReflectiveOperationException {
        Object currentPathObject = parsedObj;
        String[] pathElems = pathQry.split("/");
        ArrayList result = new ArrayList();

        for (String pathElem : pathElems) {
            pathElem = pathElem.trim();

            if (pathElem.isEmpty()) {
                continue;
            }

            //获取 list 索引位置
            if ( pathElem.indexOf("[") > -1 &&  pathElem.indexOf("]") > -1 ) {
                String[] pathElemSegms = pathElem.trim().split("\\[");

                for(int i=0;i<pathElemSegms.length;i++ ){
                    String pathElemSegm = pathElemSegms[i];
                    if(pathElemSegm.isEmpty()){
                        continue;
                    }
                    if(pathElemSegm.endsWith("]")){
                        int index = Integer.parseInt(TString.removeSuffix(pathElemSegms[i]));
                        currentPathObject = ((List)currentPathObject).get(index);
                    }else {
                        currentPathObject = (List) ((Map) currentPathObject).get(pathElemSegms[i]);
                    }
                }
            }else{
                currentPathObject =  ((Map)currentPathObject).get(pathElem);
            }


        }

        return currentPathObject;
    }


    /**
     * 获取JSONPath 对应的节点数据
     * @param pathQry JSONPath 路径
     * @param defaultValue 节点不存在时的默认值
     * @return  节点的数据
     * @throws ReflectiveOperationException 反射操作异常
     */
    public Object value(String pathQry,Object defaultValue) throws ReflectiveOperationException {
        return TObject.nullDefault(value(pathQry),defaultValue);
    }

    /**
     * 获取节点值并转换成相应的对象
     * @param pathQry  JSONPath 路径
     * @param clazz    对象的 class
     * @param <T>      范型指代对象
     * @return  转换后的对象
     * @throws ParseException  解析异常
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> T value(String pathQry, Class<T> clazz) throws ParseException, ReflectiveOperationException {
        Object value = value(pathQry);

        if(value==null){
            return null;
        }

        if (clazz.getName().startsWith("java.") || clazz.isPrimitive()) {
                return (T)TObject.cast(value);
        } else {
            Object obj = TReflect.getObjectFromMap(clazz, (Map<String, ?>) value, true);
            return TObject.cast(obj);
        }
    }

    /**
     * 获取节点值并转换成相应的对象
     * @param pathQry  JSONPath 路径
     * @param clazz    对象的 class
     * @param defaultValue 对象默认值
     * @param <T>      范型指代对象
     * @return  转换后的对象
     * @throws ParseException  解析异常
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> T value(String pathQry, Class<T> clazz, T defaultValue) throws ParseException, ReflectiveOperationException {
        return TObject.nullDefault(value(pathQry,clazz), defaultValue);
    }

    /**
     * 获取节点值并转换成相应的对象
     * @param <T>      范型指代对象
     * @param pathQry  JSONPath 路径
     * @param elemClazz    List 元素对象的 class
     * @return  转换后的对象
     * @throws ParseException  解析异常
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> List<T> listObject(String pathQry, Class<T> elemClazz) throws ParseException, ReflectiveOperationException {
        List<T> resultList = new ArrayList<T>();
        List<Map<String, ?>> listMaps = value(pathQry, List.class, TObject.newList());

        if(listMaps==null){
            return null;
        }

        for(Map<String, ?> map :listMaps){
            T obj = (T) TReflect.getObjectFromMap(elemClazz, map, true);
            resultList.add(obj);
        }

        return resultList;
    }

    /**
     * 获取节点值并转换成相应的对象
     * @param <T>      范型指代对象
     * @param pathQry  JSONPath 路径
     * @param elemClazz    List 元素对象的 class
     * @param defaultValue 节点不存在时的默认值
     * @return  转换后的对象
     * @throws ParseException  解析异常
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> List<T> listObject(String pathQry, Class<T> elemClazz,List<T> defaultValue) throws ParseException, ReflectiveOperationException {
        List<T> result = listObject(pathQry,elemClazz);
        if(result==null){
            return defaultValue;
        }else {
            return result;
        }
    }


        /**
         * 将 JSON 中的对象中的一个节点自动 转换成 java 中的对象
         * @param pathQry        JSONPath 路径
         * @param keyFieldName   key 值在 java 对象中对应的字段
         * @param elemClazz      对象的 class
         * @param <T>            范型指代对象
         * @return  转换后的对象
         * @throws ParseException  解析异常
         * @throws ReflectiveOperationException 反射异常
         */
    public <T> List<T> mapToListObject(String pathQry,String keyFieldName,Class<?> elemClazz) throws ParseException, ReflectiveOperationException {
        List<T> resultList = new ArrayList<T>();
        Map<String,?> mapValue = value(pathQry,Map.class);

        if(mapValue==null){
            return null;
        }

        for(Map.Entry<String,?> entry : mapValue.entrySet()){
            String key = entry.getKey();
            Object value = entry.getValue();
            if(value instanceof Map){
                Map map = ((Map) value);
                map.put(keyFieldName,key);
                T obj = (T) TReflect.getObjectFromMap(elemClazz, map, true);
                resultList.add(obj);
            }
        }

        return resultList;
    }

    /**
     * 将 JSON 中的对象中的一个节点自动 转换成 java 中的对象
     * @param pathQry        JSONPath 路径
     * @param keyFieldName   key 值在 java 对象中对应的字段
     * @param elemClazz      对象的 class
     * @param defaultValue   默认值
     * @param <T>            范型指代对象
     * @return  转换后的对象
     * @throws ParseException  解析异常
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> List<T> mapToListObject(String pathQry,String keyFieldName,Class<?> elemClazz,List<T> defaultValue) throws ParseException, ReflectiveOperationException {
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

package org.voovan.tools.json;

import org.voovan.tools.TObject;
import org.voovan.tools.TReflect;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
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
     * @throws ReflectiveOperationException
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

            String[] listMarks = TString.searchByRegex(pathElem, "\\[\\d+\\]$");
            int listIndex = -1;

            //获取 list 索引位置
            if (listMarks.length > 0) {
                listIndex = Integer.parseInt(TString.removeSuffix(TString.removePrefix(listMarks[0])));
            }

            if(pathElem.startsWith("root") && listIndex == -1){
                 continue;
            }

            //如果没有list索引则认为需要获取 Map,否则任务需要获取 List
            if (listIndex == -1) {
                Method mapGetMethod = TReflect.findMethod(HashMap.class, "get", new Class[]{Object.class});
                currentPathObject = TReflect.invokeMethod(currentPathObject, mapGetMethod, pathElem );
            }else {
                Method listGetMethod = TReflect.findMethod(ArrayList.class, "get", new Class[]{int.class});
                currentPathObject = TReflect.invokeMethod(currentPathObject, listGetMethod, listIndex);
            }
        }

        return currentPathObject;
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
        if (clazz.getName().startsWith("java.") || !clazz.getName().contains(".")) {
                return TObject.cast(value);
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
        T result;
        Object value = value(pathQry);
        if (clazz.getName().startsWith("java.") || !clazz.getName().contains(".")) {
            result = TObject.cast(value);
        } else {
            Object obj = TReflect.getObjectFromMap(clazz, (Map<String, ?>) value, true);
            result = TObject.cast(obj);
        }

        return TObject.nullDefault(result, defaultValue);
    }

    /**
     * 获取节点值并转换成相应的对象
     * @param pathQry  JSONPath 路径
     * @param elemClazz    List 元素对象的 class
     * @param <T>      范型指代对象
     * @return  转换后的对象
     * @throws ParseException  解析异常
     * @throws ReflectiveOperationException 反射异常
     */
    public <T> List<T> listValue(String pathQry, Class<T> elemClazz) throws ParseException, ReflectiveOperationException {
        ArrayList resultList = new ArrayList();
        List<Map<String, ?>> listMaps = value(pathQry, List.class, TObject.newList());

        for(Map<String, ?> map :listMaps){
            T obj = (T) TReflect.getObjectFromMap(elemClazz, map, true);
            resultList.add(obj);
        }

        return resultList;
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

package org.voovan.tools.json;

import org.voovan.Global;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.exception.ParseException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * JSON 使用路径解析的工具类
 *
 * @author helyho
 *         <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class BeanVisitor {

    public enum SplitChar {
        POINT("."),
        BACKSLASH(Global.STR_BACKSLASH);
        private final String value;

        SplitChar(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private Object bean;
    private SplitChar pathSplitor = SplitChar.BACKSLASH;

    public BeanVisitor(Object bean) {
        init(bean);
    }

    protected BeanVisitor() {

    }

    protected void init(Object bean) {
        if (!(bean instanceof List || bean instanceof Map)) {
            try {
                bean = TReflect.getMapFromObject(bean);
            } catch (ReflectiveOperationException e) {
                throw new ParseException("Get Map from object failed");
            }
        }
        this.bean = bean;
    }

    public Object getBean() {
        return bean;
    }

    public SplitChar getPathSplitor() {
        return pathSplitor;
    }

    public void setPathSplitor(SplitChar pathSplitor) {
        this.pathSplitor = pathSplitor;
    }

    public Object parse(String pathQry, Object parsedObj) {

        switch (pathSplitor) {
            case POINT : {
                //查找 "^"
                while(pathQry.contains("^"))
                pathQry=TString.fastReplaceAll(pathQry, "[^\\.]*\\.\\^" , "");
                break;
            }
            case BACKSLASH: {
                //查找 "../"
                while(pathQry.contains("\\.\\.\\/")) {
                    pathQry = TString.fastReplaceAll(pathQry, "[^\\/]*\\/\\.\\.\\/", "");
                }
                break;
            }

            default: ;
        }

        Object currentPathObject = parsedObj;

        String[] pathElems = pathQry.split("\\"+pathSplitor.getValue());

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
                        currentPathObject = ((List<?>) currentPathObject).get(index);
                    }else {
                        currentPathObject = (List) ((Map) currentPathObject).get(pathElemSegms[i]);
                    }
                }
            } else {
                //如果是复杂类型的特殊处理
                if(!(currentPathObject instanceof List) &&
                        !(currentPathObject instanceof Map) &&
                        !TReflect.isSystemType(currentPathObject.getClass())){
                    if(!(currentPathObject instanceof Map) && !(currentPathObject instanceof List)) {
                        try {
                            currentPathObject = TReflect.getMapFromObject(currentPathObject);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

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
                } else if(currentPathObject instanceof Map) {
                    currentPathObject = ((Map) currentPathObject).get(pathElem);
                } else {
                    return null;
                }


            }
        }

        return currentPathObject;
    }

    /**
     * 获取Bean 对应的节点数据,忽略段大小写
     * @param pathQry Bean 路径
     * @return  节点的数据
     */
    public Object value(String pathQry) {
        return parse(pathQry, bean);
    }


    /**
     * 获取Bean 对应的节点数据,默认忽略段大小写
     * @param pathQry Bean 路径
     * @param defaultValue 节点不存在时的默认值
     * @param <T>     范型
     * @return  节点的数据
     */
    public <T> T value(String pathQry, T defaultValue) {
        return (T)TObject.nullDefault(value(pathQry),defaultValue);
    }

    /**
     * 获取节点值并转换成相应的对象,默认忽略段大小写
     * @param pathQry  Bean 路径
     * @param clazz    对象的 class, Map:作为转换目标对象的类型, List 作为元素的类型
     * @param <T>      范型指代对象
     * @return  转换后的对象
     */
    public <T> T value(String pathQry, Class<T> clazz) {
        Object value = value(pathQry);
        if(clazz == null) {
            return (T)value;
        }

        if(value==null){
            return null;
        }

        if(TReflect.isSuper(value.getClass(), clazz)) {
            return (T)value;
        }

        Object obj = null;

        try {
            if(value instanceof Map) {
                obj = TReflect.getObjectFromMap(clazz, (Map<String, ?>) value, true);
            } else if(value instanceof List && TReflect.isSuper(clazz, Collection.class)) {
                obj = TReflect.getObjectFromMap(clazz, TObject.asMap(TReflect.SINGLE_VALUE_KEY, value), false);
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
            Logger.error("Parse " + pathQry + " error", e);
        }

        return (T)obj;
    }

    /**
     * 获取节点值并转换成相应的对象,忽略段大小写
     * @param pathQry  Bean 路径
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
     * @param pathQry  Bean 路径
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
            Logger.error("Parse " + pathQry + " error", e);
        }

        return null;
    }

    /**
     * 获取节点值并转换成相应的 Map 对象,忽略段大小写
     * @param <T>      范型指代对象
     * @param pathQry  Bean 路径
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
     * @param pathQry  Bean 路径
     * @param elemClazz    List 元素对象的 class
     * @return  转换后的对象
     */
    public <T> List<T> listObject(String pathQry, Class<T> elemClazz) {
        List value = (List)value(pathQry);

        if(value == null) {
            return null;
        }

        if(elemClazz == null) {
            return value;
        }

        return (List<T>) value.stream().map(item-> {
                    try {
                        if(item instanceof Map){
                            return TReflect.getObjectFromMap(elemClazz, (Map<String, ?>) item, true);
                        } else {
                            return TString.toObject(item.toString(), elemClazz);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }).filter(item->item!=null).collect(Collectors.toList());
    }

    /**
     * 获取节点值并转换成相应的对象,忽略段大小写
     * @param <T>      范型指代对象
     * @param pathQry  Bean 路径
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
     * @param pathQry        Bean 路径
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
                    Logger.error("Parse " + pathQry + " error", e);
                }
            }
        }

        return resultList;
    }

    /**
     * 将 JSON 中的对象中的一个节点自动 转换成 java 中的对象,忽略段大小写
     * @param pathQry        Bean 路径
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
     * @param obj 改造的 bean 对象
     * @return  转换后的对象
     */
    public static BeanVisitor newInstance(Object obj){
        return new BeanVisitor(obj);
    }

}

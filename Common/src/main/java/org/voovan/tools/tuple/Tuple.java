package org.voovan.tools.tuple;

import org.voovan.tools.reflect.TReflect;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 带命名的元组类型
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Tuple {
    private Integer nullValueId = 0;
    private Map<Object, TupleItem> items;


    public Tuple() {
        items = new LinkedHashMap<Object, TupleItem>();
    }

    /**
     * 为元组增加属性定义
     * @param name 属性名称
     * @param clazz 属性类型
     * @return 元组对象
     */
    public Tuple addField(String name, Class clazz) {
        items.put(name, new TupleItem(name, clazz));
        return this;
    }

    public boolean containName(String name) {
        return items.containsKey(name);
    }

    public int contain(Object value) {
        return (int)items.values().stream().filter(tupleItem -> tupleItem.equals(value)).count();
    }

    public Map<Object, TupleItem> getItems() {
        return items;
    }

    /**
     * 设置元素属性
     * @param name 属性名称
     * @param value 属性类型
     * @return 元组对象
     */
    public Tuple set(Object name, Object value) {
        boolean noneName = name == null;
        name = noneName ? nullValueId++ : name;

        TupleItem tupleItem = items.get(name);
        if(noneName && tupleItem!=null) {
            throw new TupleException("Tuple none name id duplicate: " + tupleItem);
        }

        if(tupleItem!=null) {
            if (TReflect.isSuper(value.getClass(), tupleItem.getClazz())) {
                tupleItem.setValue(value);
            } else {
                throw new TupleException("Tuple field [" + name + "] need obj " + tupleItem.getClazz() + ", actual " + value.getClass());
            }
        } else {
            Class clazz = value == null ? Object.class : value.getClass();
            items.put(name, new TupleItem(name, clazz, value));
        }
        return this;
    }

    /**
     * 设置元素属性
     * @param value 属性类型
     * @return 元组对象
     */
    public Tuple set(Object value) {
        return set(null, value);
    }

    /**
     * 获取元组元素
     * @param name 属性名称
     * @return 元组元素
     */
    public TupleItem getItem(Object name) {
        return items.get(name);
    }

    /**
     * 获取元组数据
     * @param name 属性名称
     * @param <T> 响应范型
     * @return 元组数据
     */
    public <T> T get(Object name) {
        return (T)getItem(name).getValue();
    }

    /**
     * 转换为 List
     *      list 中的元素为 TupleItem.value
     * @return List
     */
    public List toList() {
        return items.values().stream().map(tupleItem -> tupleItem.getValue()).collect(Collectors.toList());
    }

    /**
     * 转换为 Map
     *      Map 中的元素为 [TupleItem.name, TupleItem.valu]
     * @return 元组对应的 Map
     */
    public Map<String, ?> toMap() {
        Map ret = new LinkedHashMap();
        items.values().stream().forEach(tupleItem -> ret.put(tupleItem.getName(), tupleItem.getValue()));
        return ret;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "items=" + items +
                '}';
    }

    /**
     * 参数构造
     * @param items 参数
     * @return 元组对象
     */
    public static Tuple with(Object ... items) {
        Tuple tuple =  new Tuple();
        for(Object item : items) {
            tuple.set(item);
        }
        return tuple;
    }

    /**
     * 名称, 参数构造
     * @param items 命名参数, 类似:[name_1, val_1, name_2, val_2...name_n, val_n]
     * @return 元组对象
     */
    public static Tuple withName(Object ... items) {
        Tuple tuple =  new Tuple();
        for(int i=0;i<items.length;i++) {
            String name = items[i].toString();
            Object value = i < items.length ? items[++i] : null;
            tuple.set(name, value);
        }
        return tuple;
    }


}

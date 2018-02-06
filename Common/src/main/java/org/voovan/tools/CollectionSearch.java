package org.voovan.tools;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Collection 筛选类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class CollectionSearch {
    private Collection collection;
    private final Map<String, Object> conditions = new ConcurrentHashMap<String,  Object>();
    private String sortField = null;
    private boolean isDesc = true;
    private int pageNum = 0;
    private int pageSize = 0;

    /**
     * 构造函数
     * @param collection 将被用于筛选 Collection 对象
     */
    public CollectionSearch(Collection collection){
        this.collection = collection;
    }

    /**
     * 增加一个筛选条件
     * @param field 被筛选字段
     * @param value 筛选的值
     * @return CollectionSearch 对象
     */
    public CollectionSearch addCondition(String field, Object value){
        conditions.put(field, value);
        return this;
    }

    /**
     * 将筛选出的数据排序
     * @param sortField 排序字段
     * @param isDesc 排序方式: false: 从大到小, true: 从小到大
     * @return CollectionSearch 对象
     */
    public CollectionSearch sort(String sortField, boolean isDesc){
        this.sortField = sortField;
        this.isDesc = isDesc;
        return this;
    }

    /**
     * 将筛选出的数据分页
     * @param pageNum   页面
     * @param pageSize  页面数据量
     * @return CollectionSearch 对象
     */
    public CollectionSearch page(int pageNum, int pageSize){
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        return this;
    }

    /**
     * 筛选数据
     * @return 筛选得到的数据
     */
    public Collection search(){
        List listResult = (List)collection.stream().filter(new Predicate(){
            @Override
            public boolean test(Object o) {
                boolean isMatch = true;
                for(Map.Entry<String, Object> entry : conditions.entrySet()){
                    Object collectionValue = null;
                    try {
                        collectionValue = TReflect.getFieldValue(o, entry.getKey());
                        if(!entry.getValue().equals(collectionValue)){
                            isMatch = false;
                            break;
                        }
                    } catch (ReflectiveOperationException e) {
                        Logger.error("CollectionSearch filter error", e);
                        isMatch = false;
                    }

                }
                return isMatch;
            }
        }).sorted(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                try {
                    Object fieldValue1 = TReflect.getFieldValue(o1, sortField);
                    Object fieldValue2 = TReflect.getFieldValue(o2, sortField);
                    if(fieldValue1 instanceof Number && fieldValue2 instanceof Number){
                        BigDecimal value1 = new BigDecimal(fieldValue1.toString());
                        BigDecimal value2 = new BigDecimal(fieldValue2.toString());
                        if(isDesc) {
                            return value1.compareTo(value2);
                        } else {
                            return value1.compareTo(value2) * -1;
                        }
                    } else {
                        return 0;
                    }
                } catch (ReflectiveOperationException e) {
                    Logger.error("CollectionSearch sorted error", e);
                    return 0;
                }
            }
        }).collect(Collectors.toList());

        if(pageNum>0 && pageSize>0) {
            int start = (pageNum-1)*pageSize;
            int end = start + pageSize;
            listResult = listResult.subList(start, end);
        }

        return listResult;
    }

    /**
     * 静态构造方法
     * @param collection 将被用于筛选 Collection 对象
     * @return CollectionSearch 对象
     */
    public static CollectionSearch newInstance(Collection collection){
        return new CollectionSearch(collection);
    }
}

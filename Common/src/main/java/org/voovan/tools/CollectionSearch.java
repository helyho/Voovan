package org.voovan.tools;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Map<String, Map<String, Object>> searchStep = new TreeMap<String, Map<String, Object>>();
    int step=0;

    /**
     * 构造函数
     *
     * @param collection 将被用于筛选 Collection 对象
     */
    public CollectionSearch(Collection collection) {
        this.collection = collection;
    }

    /**
     * 增加一个筛选条件
     *
     * @param field 被筛选字段
     * @param value 筛选的值
     * @return CollectionSearch 对象
     */
    public CollectionSearch addCondition(String field, Object value) {
        searchStep.put( (step++)+"_Condition", TObject.asMap(field, value));
        return this;
    }

    /**
     * 将筛选出的数据排序
     *
     * @param sortField 排序字段
     * @param isAsc     排序方式: false: 从大到小, true: 从小到大
     * @return CollectionSearch 对象
     */
    public CollectionSearch sort(String sortField, boolean isAsc) {
        searchStep.put((step++)+"_Sort", TObject.asMap(sortField, isAsc));
        return this;
    }

    /**
     * 将筛选出的数据排序
     *
     * @param sortField 排序字段
     * @return CollectionSearch 对象
     */
    public CollectionSearch sort(String sortField) {
        searchStep.put((step++)+"_Sort", TObject.asMap(sortField, true));
        return this;
    }

    /**
     * 限制集合的记录数
     *
     * @param limit 记录数
     * @return CollectionSearch 对象
     */
    public CollectionSearch limit(int limit) {
        searchStep.put((step++)+"_Limit", TObject.asMap("limit", limit));
        return this;
    }

    /**
     * 将筛选出的数据分页
     *
     * @param pageNum  页面
     * @param pageSize 页面数据量
     * @return CollectionSearch 对象
     */
    public CollectionSearch page(int pageNum, int pageSize) {
        searchStep.put((step++)+"_Page", TObject.asMap("pageNum", pageNum, "pageSize", pageSize));
        return this;
    }

    /**
     * 筛选数据
     *
     * @return 筛选得到的数据
     */
    public Collection search() {

        Stream stream = collection.stream();

        for (Map.Entry<String, Map<String, Object>> step : searchStep.entrySet()) {
            String stepType = step.getKey();
            Map.Entry<String, Object> stepMap = step.getValue().entrySet().iterator().next();

            String stepMapKey = stepMap.getKey();
            Object stepMapValue = stepMap.getValue();

            //筛选条件
            if (stepType.endsWith("Condition")) {
                stream = stream.filter(new Predicate() {
                    @Override
                    public boolean test(Object o) {
                        boolean isMatch = true;
                        Object collectionValue = null;
                        try {
                            collectionValue = TReflect.getFieldValue(o, stepMapKey);

                            if (!stepMapValue.equals(collectionValue)) {
                                isMatch = false;
                            }
                        } catch (ReflectiveOperationException e) {
                            Logger.error("CollectionSearch filter error", e);
                            isMatch = false;
                        }

                        return isMatch;
                    }
                });
            }
            //排序
            else if (stepType.endsWith("Sort")) {
                final String sortField = stepMapKey;
                final boolean isAsc = (Boolean) stepMapValue;
                stream = stream.sorted(new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        try {
                            Object fieldValue1 = TReflect.getFieldValue(o1, sortField);
                            Object fieldValue2 = TReflect.getFieldValue(o2, sortField);
                            if (fieldValue1 instanceof Number && fieldValue2 instanceof Number) {
                                BigDecimal value1 = new BigDecimal(fieldValue1.toString());
                                BigDecimal value2 = new BigDecimal(fieldValue2.toString());
                                if (isAsc) {
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
                });
            }
            //数量限制
            else if (stepType.endsWith("Limit")) {
                final int limit = (Integer) stepMapValue;
                stream = stream.limit(limit);
            }
            //分页
            else if(stepType.endsWith("Page")){
                int pageNum = (Integer) step.getValue().get("pageNum");
                int pageSize = (Integer) step.getValue().get("pageSize");

                List listResult = (List) stream.collect(Collectors.toList());


                //分页
                if (pageNum > 0 && pageSize > 0) {
                    int start = (pageNum - 1) * pageSize;
                    int end = start + pageSize;

                    if(end > listResult.size()){
                        end = listResult.size()-1;
                    }
                    listResult = listResult.subList(start, end);
                }

                stream = listResult.stream();
            }
        }

        //结果集
        List listResult = (List) stream.collect(Collectors.toList());

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

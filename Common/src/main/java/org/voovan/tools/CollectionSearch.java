package org.voovan.tools;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.*;
import java.util.function.Function;
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
public class CollectionSearch<T> {
    private int step = 0;
    private boolean parallelStream = true;
    private Collection<T> collection;
    private Map<String, Map<String, Object>> searchStep = new TreeMap<String, Map<String, Object>>();

    /**
     * 构造函数
     *
     * @param collection 将被用于筛选 Collection 对象
     */
    public CollectionSearch(Collection<T> collection) {
        this.collection = collection;
    }

    /**
     * 静态构造方法
     *
     * @param collection 将被用于筛选 Collection 对象
     * @param <P> 集合元素的范型
     * @return CollectionSearch 对象
     *
     */

    public static <P> CollectionSearch<P> newInstance(Collection<P> collection) {
        return new CollectionSearch<P>(collection);
    }


    /**
     * 设置是否并行处理
     * @return true:并行处理, false: 单线程处理
     */
    public boolean isParallelStream() {
        return parallelStream;
    }

    /**
     * 设置是否并行处理
     * @param parallelStream true:并行处理, false: 单线程处理
     * @return CollectionSearch 对象
     */
    public CollectionSearch setParallelStream(boolean parallelStream) {
        this.parallelStream = parallelStream;
        return this;
    }

    /**
     * 增加一个筛选条件
     *
     * @param field 被筛选字段
     * @param value 筛选的值
     * @return CollectionSearch 对象
     */
    public CollectionSearch addCondition(String field, Object value) {
        searchStep.put((step++) + "_Condition", TObject.asMap("field", field, "value", value, "operate", Operate.EQUAL));
        return this;
    }

    /**
     * 增加一个筛选条件
     *
     * @param field 被筛选字段
     * @param operate 操作符美居
     * @param value 筛选的值
     * @return CollectionSearch 对象
     */
    public CollectionSearch addCondition(String field, Operate operate, Object value) {
        searchStep.put((step++) + "_Condition", TObject.asMap("field", field, "value", value, "operate", operate));
        return this;
    }

    /**
     * 增加一个筛选条件
     *
     * @param predicate 筛选函数
     * @return CollectionSearch 对象
     */
    public CollectionSearch addCondition(Predicate<T> predicate) {
        searchStep.put((step++) + "_Condition", TObject.asMap("predicate", predicate));
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
        searchStep.put((step++) + "_Sort", TObject.asMap("field", sortField, "isAsc", isAsc));
        return this;
    }

    /**
     * 将筛选出的数据排序
     *
     * @param sortField 排序字段
     * @return CollectionSearch 对象
     */
    public CollectionSearch sort(String sortField) {
        searchStep.put((step++) + "_Sort", TObject.asMap("field", sortField, "isAsc", true));
        return this;
    }

    /**
     * 将筛选出的数据排序
     *
     * @param comparator 排序函数
     * @return CollectionSearch 对象
     */
    public CollectionSearch sort(Comparator<T> comparator) {
        searchStep.put((step++) + "_Sort", TObject.asMap("comparator", comparator));
        return this;
    }

    /**
     * 限制集合的记录数
     *
     * @param limit 记录数
     * @return CollectionSearch 对象
     */
    public CollectionSearch limit(int limit) {
        searchStep.put((step++) + "_Limit", TObject.asMap("limit", limit));
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
        searchStep.put((step++) + "_Page", TObject.asMap("pageNum", pageNum, "pageSize", pageSize));
        return this;
    }

    public Stream streamRun(){

        Stream stream = null;
        if(this.parallelStream) {
            stream = collection.parallelStream();
        } else {
            stream = collection.stream();
        }

        for (Map.Entry<String, Map<String, Object>> step : searchStep.entrySet()) {
            String stepType = step.getKey();
            Map<String, Object> stepInfo = step.getValue();

            //筛选条件
            if (stepType.endsWith("Condition")) {
                if(stepInfo.get("predicate")!=null){
                    stream = stream.filter((Predicate)stepInfo.get("predicate"));
                } else {
                    stream = stream.filter(new Predicate<T>() {
                        @Override
                        public boolean test(T o) {
                            boolean isMatch = false;
                            Object collectionValue = null;
                            try {
                                String stepField = (String) stepInfo.get("field");
                                Object stepValue = stepInfo.get("value");
                                Operate stepOperate = (Operate) stepInfo.get("operate");

                                if(o==null){
                                    return false;
                                }

                                if(o instanceof Map){
                                    collectionValue = ((Map)o).get(stepField);
                                } else {
                                    collectionValue = TReflect.getFieldValue(o, stepField);
                                }

                                if(collectionValue==null){
                                    return false;
                                }

                                if (collectionValue.getClass().getSimpleName().startsWith("Atomic")) {
                                    collectionValue = TReflect.invokeMethod(collectionValue, "get");
                                }

                                if (collectionValue == null && stepValue == null) {
                                    isMatch = true;
                                } else if (collectionValue.getClass().equals(stepValue.getClass())) {

                                    //带有比较器
                                    if (stepValue instanceof Comparable) {
                                        if (stepOperate == Operate.EQUAL || stepOperate == Operate.GREATER || stepOperate == Operate.LESS || stepOperate == Operate.NOT_EQUAL) {

                                            int compareResult = ((Comparable) collectionValue).compareTo(stepValue);


                                            if (compareResult == 0 && stepOperate == Operate.EQUAL) {
                                                isMatch = true;
                                            } else if (compareResult != 0 && stepOperate == Operate.NOT_EQUAL) {
                                                isMatch = true;
                                            } else if (compareResult > 0 && stepOperate == Operate.GREATER) {
                                                isMatch = true;
                                            } else if (compareResult < 0 && stepOperate == Operate.LESS) {
                                                isMatch = true;
                                            }

                                        }
                                    } else {
                                        isMatch = false;
                                    }

                                    //字符串的特有比较
                                    if (stepValue instanceof String && !isMatch) {
                                        String stringStepValue = (String) stepValue;
                                        String stringCollectionValue = (String) collectionValue;

                                        if (stepOperate == Operate.START_WITH) {
                                            isMatch = stringCollectionValue.startsWith(stringStepValue);
                                        } else if (stepOperate == Operate.END_WITH) {
                                            isMatch = stringCollectionValue.endsWith(stringStepValue);
                                        } else if (stepOperate == Operate.CONTAINS) {
                                            isMatch = stringCollectionValue.contains(stringStepValue);
                                        } else {
                                            isMatch = false;
                                        }
                                    }

                                    //集合比较
                                    if (stepValue instanceof Collection && !isMatch) {

                                        Collection collectionStepValue = (Collection) collectionValue;

                                        if (stepOperate == Operate.CONTAINS) {
                                            isMatch = collectionStepValue.contains(stepValue);
                                        } else {
                                            isMatch = false;
                                        }
                                    }

                                } else {
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
            }
            //排序
            else if (stepType.endsWith("Sort")) {
                if(stepInfo.get("comparator")!=null){
                    stream = stream.sorted((Comparator)stepInfo.get("comparator"));
                } else {
                    final String sortField = (String) stepInfo.get("field");
                    final boolean isAsc = (Boolean) stepInfo.get("isAsc");
                    stream = stream.sorted(new Comparator<T>() {
                        @Override
                        public int compare(T o1, T o2) {
                            try {
                                Object fieldValue1 = null;
                                Object fieldValue2 = null;

                                if(o1 == null || o2 == null){
                                    return -1;
                                }

                                if(fieldValue1 instanceof Map){
                                    fieldValue1 = ((Map)o1).get(sortField);
                                } else {
                                    fieldValue1 = TReflect.getFieldValue(o1, sortField);
                                }

                                if(fieldValue2 instanceof Map){
                                    fieldValue2 = ((Map)o2).get(sortField);

                                } else {
                                    fieldValue2 = TReflect.getFieldValue(o2, sortField);
                                }

                                if(fieldValue1==null || fieldValue2==null){
                                    return -1;
                                }

                                if (fieldValue1.getClass() == fieldValue2.getClass()) {
                                    if (fieldValue1 instanceof Comparable) {

                                        if (isAsc) {
                                            return ((Comparable) fieldValue1).compareTo(fieldValue2);
                                        } else {
                                            return ((Comparable) fieldValue1).compareTo(fieldValue2) * -1;
                                        }
                                    } else {
                                        return 0;
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
            }

            //数量限制
            else if (stepType.endsWith("Limit")) {
                final int limit = (Integer) stepInfo.get("limit");
                stream = stream.limit(limit);
            }
            //分页
            else if (stepType.endsWith("Page")) {
                int pageNum = (Integer) stepInfo.get("pageNum");
                int pageSize = (Integer) stepInfo.get("pageSize");

                List listResult = (List) stream.collect(Collectors.toList());

                //分页
                if (pageNum > 0 && pageSize > 0) {
                    int start = (pageNum - 1) * pageSize;
                    int end = start + pageSize;

                    if (end > listResult.size()) {
                        end = listResult.size();
                    }

                    if(end > listResult.size()-1){
                        end = listResult.size();
                    }

                    if(start < end) {
                        listResult = listResult.subList(start, end);
                    } else {
                        listResult = new ArrayList();
                    }
                }

                stream = listResult.stream();
            }
        }

        return stream;
    }

    /**
     * 筛选数据
     *
     * @return 筛选得到的数据
     */
    public Collection<T> search() {
        //结果集
        List<T> listResult = (List<T>) streamRun().collect(Collectors.toList());
        return listResult;
    }

    /**
     * 筛选数据
     * @param fieldFilters 字段集合
     * @param <R> 范型类型
     * @return 筛选得到的数据
     */
    public <R> List<R> fields(String... fieldFilters) {
        //结果集
        List<R> listResult = (List<R>) streamRun().map(new Function() {
            @Override
            public Object apply(Object o) {
                return TReflect.fieldFilter(o, fieldFilters);
            }
        }).collect(Collectors.toList());
        return listResult;
    }

    /**
     * 筛选数据
     * @param function map 的业务函数 function 对象
     * @param <R> 范型类型
     * @return 筛选得到的数据
     */
    public <R> List<R> map(Function<T, R> function) {
        //结果集
        List<R> listResult = (List<R>) streamRun().map(function).collect(Collectors.toList());
        return listResult;
    }

    /**
     * 筛选数据,并扁平化操作
     * @param function flatMap 的业务函数 function 对象
     * @param <R> 范型类型
     * @return 筛选得到的数据
     */
    public <R> List<R> flatMap(Function<T, R> function) {
        //结果集
        List<R> listResult = (List<R>) streamRun().map(function).collect(Collectors.toList());
        return listResult;
    }

    /**
     * 获取筛选数据的数量
     *
     * @return 筛选得到的数据
     */
    public long count() {
        return streamRun().count();
    }

    public enum Operate {
        EQUAL, NOT_EQUAL, GREATER, LESS, START_WITH, END_WITH, CONTAINS
    }
}

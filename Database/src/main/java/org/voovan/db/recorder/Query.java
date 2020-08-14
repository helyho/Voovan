package org.voovan.db.recorder;

import org.voovan.db.recorder.exception.RecorderException;

import java.util.*;

/**
 * 查询条件构造
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class Query {
    private List<String> dataFields;
    private Map<String, Operate> andFields;
    private Map<String, Operate> orFields;
    private Map<String[], Boolean> orderFields;
    private List<String> customCondictions;
    private int pageNumber = -1;
    private int pageSize = -1;

    public Query() {
        dataFields = new ArrayList<String>();
        andFields = new IdentityHashMap<String, Operate>();
        orFields = new IdentityHashMap<String, Operate>();
        orderFields = new IdentityHashMap<String[], Boolean>();
        customCondictions = new ArrayList<String>();
    }

    public boolean hasCondiction() {
        return !andFields.isEmpty() || !orFields.isEmpty() || !customCondictions.isEmpty();
    }

    public Query data(String ... fieldNameArr) {
        if(fieldNameArr != null) {
            for (String fieldName : fieldNameArr) {
                dataFields.add(fieldName);
            }
        }
        return this;
    }

    public Query and(String fieldName, Operate operator) {
        andFields.put(fieldName, operator);
        return this;
    }

    public Query or(String fieldName, Operate operator) {
        orFields.put(fieldName, operator);
        return this;
    }

    public Query and(String ... fieldNameArr) {
        if(fieldNameArr != null) {
            for (String fieldName : fieldNameArr) {
                andFields.put(fieldName, Operate.EQUAL);
            }
        }
        return this;
    }

    public Query or(String ... fieldNameArr) {
        if(fieldNameArr != null) {
            for (String fieldName : fieldNameArr) {
                orFields.put(fieldName, Operate.EQUAL);
            }
        }
        return this;
    }

    public Query custom(String ... customCondictionArr){
        if(customCondictionArr != null) {
            for (String customCondiction : customCondictionArr) {
                customCondictions.add(customCondiction);
            }
        }
        return this;
    }

    public Query order(String ... fieldNames) {
        orderFields.put(fieldNames, false);
        return this;
    }

    public Query order(Boolean isDesc, String ... fieldNames) {
        orderFields.put(fieldNames, isDesc);
        return this;
    }

    public Query page(int pageNumber, int pageSize){
        if(pageNumber <=0 ) {
            throw new RecorderException("pageNumber must be > 0");
        }

        if(pageNumber <=0 ) {
            throw new RecorderException("pageSize must be > 0");
        }

        this.pageNumber = pageNumber;
        this.pageSize = pageSize;

        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        if(pageNumber <=0 ) {
            throw new RecorderException("pageNumber must be > 0");
        }
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        if(pageNumber <=0 ) {
            throw new RecorderException("pageSize must be > 0");
        }
        this.pageSize = pageSize;
    }

    public List<String> getDataFields() {
        return dataFields;
    }

    public Map<String, Operate> getAndFields() {
        return andFields;
    }

    public Map<String, Operate> getOrFields() {
        return orFields;
    }

    public List<String> getCustomCondictions() {
        return customCondictions;
    }

    protected Map<String[], Boolean> getOrderFields() {
        return orderFields;
    }

    public enum Operate{
        EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, LIKE, IN, NOT_LIKE, NOT_IN
    }

    public static String getActualOperate(Operate operate) throws RecorderException {
        switch (operate){
            case EQUAL : return "=";
            case NOT_EQUAL : return "!=";
            case GREATER : return ">";
            case LESS : return "<";
            case GREATER_EQUAL : return ">=";
            case LESS_EQUAL : return "<=";
            case LIKE : return "like";
            case IN : return "in";
            case NOT_LIKE : return "not like";
            case NOT_IN : return "not in";
            default : throw new RecorderException("operate is unknow");
        }
    }

    public static Query newInstance(){
        return new Query();
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataFields, andFields, orFields, orderFields, customCondictions, pageNumber, pageSize);
    }
}

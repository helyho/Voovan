package org.voovan.db.recorder;

import org.voovan.db.recorder.exception.RecorderException;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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

    public Query data(String fieldName) {
        dataFields.add(fieldName);
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

    public Query and(String fieldName) {
        andFields.put(fieldName, Operate.EQUAL);
        return this;
    }

    public Query or(String fieldName) {
        orFields.put(fieldName, Operate.EQUAL);
        return this;
    }

    public Query custom(String customCondiction){
        customCondictions.add(customCondiction);
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
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;

        return this;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    protected List<String> getDataFields() {
        return dataFields;
    }

    protected Map<String, Operate> getAndFields() {
        return andFields;
    }

    protected Map<String, Operate> getOrFields() {
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
}

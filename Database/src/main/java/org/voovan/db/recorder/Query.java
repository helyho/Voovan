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
    private List<String> resultFields;
    private Map<String, Operate> queryAndFields;
    private Map<String, Operate> queryOrFields;
    private Map<String[], Boolean> orderFields;
    private List<String> customCondictions;
    private int pageNumber = -1;
    private int pageSize = -1;

    public Query() {
        resultFields = new ArrayList<String>();
        queryAndFields = new IdentityHashMap<String, Operate>();
        queryOrFields = new IdentityHashMap<String, Operate>();
        orderFields = new IdentityHashMap<String[], Boolean>();
        customCondictions = new ArrayList<String>();
    }

    public Query addResult(String fieldName) {
        resultFields.add(fieldName);
        return this;
    }

    public Query addAnd(String fieldName, Operate operator) {
        queryAndFields.put(fieldName, operator);
        return this;
    }

    public Query AddOr(String fieldName, Operate operator) {
        queryOrFields.put(fieldName, operator);
        return this;
    }

    public Query addAnd(String fieldName) {
        queryAndFields.put(fieldName, Operate.EQUAL);
        return this;
    }

    public Query AddOr(String fieldName) {
        queryOrFields.put(fieldName, Operate.EQUAL);
        return this;
    }

    public Query addCustomCondiction(String customCondiction){
        customCondictions.add(customCondiction);
        return this;
    }

    public Query addOrder(String ... fieldNames) {
        orderFields.put(fieldNames, false);
        return this;
    }

    public Query addOrder(Boolean isDesc, String ... fieldNames) {
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

    protected List<String> getResultFields() {
        return resultFields;
    }

    protected Map<String, Operate> getQueryAndFields() {
        return queryAndFields;
    }

    protected Map<String, Operate> getQueryOrFields() {
        return queryOrFields;
    }


    public List<String> getCustomCondictions() {
        return customCondictions;
    }

    protected Map<String[], Boolean> getOrderFields() {
        return orderFields;
    }

    public enum Operate{
        EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, LIKE, IN
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
            default : throw new RecorderException("operate is unknow");
        }
    }

    public static Query newInstance(){
        return new Query();
    }
}

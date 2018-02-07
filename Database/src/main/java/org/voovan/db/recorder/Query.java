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
    private List<String> resultField;
    private Map<String, Operate> queryAndField;
    private Map<String, Operate> queryOrField;
    private Map<String[], Boolean> orderField;

    public Query() {
        resultField = new ArrayList<>();
        queryAndField = new IdentityHashMap<String, Operate>();
        queryOrField = new IdentityHashMap<String, Operate>();
        orderField = new IdentityHashMap<String[], Boolean>();
    }

    public Query addResult(String fieldName) {
        resultField.add(fieldName);
        return this;
    }

    public Query addAnd(String fieldName, Operate operator) {
        queryAndField.put(fieldName, operator);
        return this;
    }

    public Query AddOr(String fieldName, Operate operator) {
        queryOrField.put(fieldName, operator);
        return this;
    }

    public Query addAnd(String fieldName) {
        queryAndField.put(fieldName, Operate.EQUAL);
        return this;
    }

    public Query AddOr(String fieldName) {
        queryOrField.put(fieldName, Operate.EQUAL);
        return this;
    }

    public Query addOrder(Boolean isDesc, String ... fieldNames) {
        orderField.put(fieldNames, isDesc);
        return this;
    }

    protected List<String> getResultField() {
        return resultField;
    }

    protected Map<String, Operate> getQueryAndField() {
        return queryAndField;
    }

    protected Map<String, Operate> getQueryOrField() {
        return queryOrField;
    }

    protected Map<String[], Boolean> getOrderField() {
        return orderField;
    }

    public enum Operate{
        EQUAL, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, LIKE, IN
    }

    public static String getActualOperate(Operate operate) throws RecorderException {
        switch (operate){
            case EQUAL : return "=";
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

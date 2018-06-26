package org.voovan.tools.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis 的扫描结果封装对象
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class ScanedObject<V> {
    private String cursor;
    private List<V> resultList;

    public ScanedObject(String cursor) {
        this.cursor = cursor;
        this.resultList = new ArrayList<V>();
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public List<V> getResultList() {
        return resultList;
    }

    public void setResultList(List<V> resultList) {
        this.resultList = resultList;
    }
}

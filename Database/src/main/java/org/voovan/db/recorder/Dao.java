package org.voovan.db.recorder;

import org.voovan.db.JdbcOperate;
import org.voovan.db.recorder.annotation.NotInsert;
import org.voovan.db.recorder.annotation.NotUpdate;
import org.voovan.db.recorder.exception.RecorderException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.reflect.annotation.NotSerialization;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Dao 对象基础类
 *
 * @author: helyho
 * walletbridge Framework.
 * WebSite: https://github.com/helyho/walletbridge
 * Licence: Apache v2 License
 */
public class Dao<T> {
    public static String[] emptyStringArray = new String[0];

    @NotUpdate
    @NotInsert
    @NotSerialization
    public transient DataSource dataSource;

    @NotUpdate
    @NotInsert
    @NotSerialization
    public transient Map<String, Object> snapshot;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean insert() {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        return recorder.insert((T)this) == 1;
    }

    public void snapshot() {
        try {
            snapshot = TReflect.getMapfromObject(this);
        } catch (ReflectiveOperationException e) {
            throw new RecorderException("beginUpdate failed", e);
        }
    }

    private String[] getModifyField() {
        try {
            Map<String, Object> current = TReflect.getMapfromObject(this);

            List<String> modifyField = new ArrayList<String>();
            for (Map.Entry<String, Object> entry : current.entrySet()) {
                Object originFieldValue = snapshot.get(entry.getKey());
                Object currentFieldValue = entry.getValue();

                if (originFieldValue == null && currentFieldValue == null) {
                    continue;
                } else if (originFieldValue != null && !originFieldValue.equals(currentFieldValue)) {
                    modifyField.add(entry.getKey());
                } else if (currentFieldValue != null && !currentFieldValue.equals(originFieldValue)) {
                    modifyField.add(entry.getKey());
                }
            }

            snapshot = null;
            return modifyField.toArray(emptyStringArray);
        } catch (ReflectiveOperationException e) {
            throw new RecorderException("endUpdate failed", e);
        }
    }

    public boolean update(String ... dataFields) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().data(dataFields);
        return recorder.update((T)this, query) == 1;
    }

    public boolean update() {
        if(snapshot !=null) {
            return update(getModifyField());
        } else {
            return update(null);
        }
    }

    public List<T> query(String[] dataFields, String[] andFields, Integer pageNum, Integer pageSize) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().data(dataFields).and(andFields);
        pageNum = pageNum == null ? -1 : pageNum;
        pageSize = pageSize == null ? -1 : pageSize;
        query.page(pageNum, pageSize);

        return recorder.query((T)this, query);
    }

    public List<T> query(String[] andFields, Integer pageNum, Integer pageSize) {
        return query(null, andFields, pageNum, pageSize);
    }

    public List<T> query(String[] dataFields, String[] andFields) {
        return query(dataFields, andFields, null, null);
    }

    public List<T> query(String ... andFields) {
        return query(null, andFields, null, null);
    }

    public List<T> query() {
        if(snapshot !=null) {
            return query(null, getModifyField(), null, null);
        } else {
            return query(null, null, null, null);
        }
    }

    public T queryOne(String[] dataFields, String[] andFields, Integer pageNum, Integer pageSize) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().data(dataFields).and(andFields);
        pageNum = pageNum == null ? -1 : pageNum;
        pageSize = pageSize == null ? -1 : pageSize;
        query.page(pageNum, pageSize);

        List<T> results = recorder.query((T)this, query);

        if(results.size()==0) {
            return null;
        } else if(results.size() == 1) {
            return results.get(0);
        } else {
            Logger.warn("query return more than one result");
            return results.get(0);
        }
    }

    public T queryOne(String[] andFields, Integer pageNum, Integer pageSize) {
        return queryOne(null, andFields, pageNum, pageSize);
    }

    public List<T> queryOne(String[] dataFields, String[] andFields) {
        return query(dataFields, andFields, null, null);
    }

    public T queryOne(String ... andFields) {
        return queryOne(null, andFields, null, null);
    }

    public T queryOne() {
        if(snapshot !=null) {
            return queryOne(null, getModifyField(), null, null);
        } else {
            return queryOne(null, null, null, null);
        }
    }


    public int count(String... andFields) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().and(andFields);
        return recorder.count((T)this, query);
    }

    public int count() {
        if(snapshot !=null) {
            return count(getModifyField());
        } else {
            return count(null);
        }
    }

    public boolean delete(String ... andFields) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().and(andFields);

        return recorder.delete((T)this, query) == 1;
    }

    public boolean delete() {
        if(snapshot !=null) {
            return delete(getModifyField());
        } else {
            return delete(null);
        }
    }
}

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
 * 类文字命名
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
    public transient Map<String, Object> originMap;

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

    public void beginUpdate() {
        try {
            originMap = TReflect.getMapfromObject(this);
        } catch (ReflectiveOperationException e) {
            throw new RecorderException("beginUpdate failed", e);
        }
    }

    public void endUpdate() {
        try {
            Map<String, Object> currentMap = TReflect.getMapfromObject(this);

            List<String> modifyField = new ArrayList<String>();
            for(Map.Entry<String, Object> entry : currentMap.entrySet()) {
                Object originFieldValue = originMap.get(entry.getKey());
                Object currentFieldValue = entry.getValue();

                if(originFieldValue==null && currentFieldValue==null) {
                    continue;
                } else if(originFieldValue!=null && !originFieldValue.equals(currentFieldValue)) {
                    modifyField.add(entry.getKey());
                } else if(currentFieldValue!=null && !currentFieldValue.equals(originFieldValue)) {
                    modifyField.add(entry.getKey());
                }
            }

            update(modifyField.toArray(emptyStringArray));
            originMap = null;
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
        return update(null);
    }

    public List<T> query(String[] dataFields, String[] andFields) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().data(dataFields).and(andFields);
        return recorder.query((T)this, query);
    }

    public List<T> query(String ... andFields) {
        return query(null, andFields);
    }

    public List<T> query() {
        return query(null, null);
    }

    public T queryOne(String[] dataFields, String[] andFields) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().data(dataFields).and(andFields);
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

    public T queryOne(String ... andFields) {
        return queryOne(null, andFields);
    }

    public T queryOne() {
        return queryOne(null, null);
    }


    public boolean delete(String ... andFields) {
        Recorder recorder = new Recorder(new JdbcOperate(dataSource));
        Query query = Query.newInstance().and(andFields);

        return recorder.delete((T)this, query) == 1;
    }

    public boolean delete() {
        return delete(null);
    }
}

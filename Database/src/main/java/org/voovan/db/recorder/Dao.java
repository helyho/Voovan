package org.voovan.db.recorder;

import org.voovan.db.JdbcOperate;
import org.voovan.db.TranscationType;
import org.voovan.db.recorder.annotation.NotInsert;
import org.voovan.db.recorder.annotation.NotUpdate;
import org.voovan.db.recorder.exception.RecorderException;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.reflect.annotation.NotSerialization;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Dao 对象基础类
 *
 * @author: helyho
 * walletbridge Framework.
 * WebSite: https://github.com/helyho/walletbridge
 * Licence: Apache v2 License
 */
public class Dao<T extends Dao> {
    private static String[] emptyStringArray = new String[0];

    @NotUpdate
    @NotInsert
    @NotSerialization
    private transient JdbcOperate jdbcOperate;

    @NotUpdate
    @NotInsert
    @NotSerialization
    private transient Recorder recorder;

    @NotUpdate
    @NotInsert
    @NotSerialization
    private transient Map<String, Object> snapshot;

    public JdbcOperate getJdbcOperate() {
        return jdbcOperate;
    }

    public T setJdbcOperate(JdbcOperate jdbcOperate) {
        if(jdbcOperate == null) {
            throw new RecorderException("JdbcOperate must not null");
        }
        this.recorder = new Recorder(jdbcOperate);
        jdbcOperate.setTranscationType(TranscationType.ALONE);
        this.jdbcOperate = jdbcOperate;

        return (T) this;
    }

    public Recorder getRecorder() {
        return recorder;
    }

    public T setRecorder(Recorder recorder) {
        if(recorder == null) {
            throw new RecorderException("Recorder must not null");
        }
        this.jdbcOperate = recorder.getJdbcOperate();
        this.recorder = recorder;

        return (T) this;
    }

    public T snapshot() {
        try {
            snapshot = TReflect.getMapfromObject(this, true);
            return (T)this;
        } catch (ReflectiveOperationException e) {
            throw new RecorderException("beginUpdate failed", e);
        }
    }

    private String[] getModifyField() {
        try {
            Map<String, Object> current = TReflect.getMapfromObject(this, true);

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


    public boolean insert() {
        try {
            if (recorder.insert((T)this) == 1) {
                recorder.getJdbcOperate().commit();
                return true;
            } else {
                recorder.getJdbcOperate().rollback();
                return false;
            }
        } catch (Exception e) {
            try {
                recorder.getJdbcOperate().rollback();
            } catch (SQLException throwables) {
                Logger.error("Dao.update exception rollback failed", e);
            }

            Logger.error("Dao.update failed", e);
            return false;
        }
    }

    public boolean update(String[] dataFields, String[] andFields, int effectRow) {
        try {
            Query query = Query.newInstance().data(dataFields).and(andFields);
            if (recorder.update((T) this, query) == effectRow) {
                recorder.getJdbcOperate().commit();
                return true;
            } else {
                recorder.getJdbcOperate().rollback();
                return false;
            }
        } catch (Exception e) {
            try {
                recorder.getJdbcOperate().rollback();
            } catch (SQLException throwables) {
                Logger.error("Dao.update exception rollback failed", e);
            }
            Logger.error("Dao.update failed", e);
            return false;
        }
    }

    public boolean update(String[] dataFields, int effectRow) {
        return update(dataFields, null, effectRow);
    }

    public boolean update(String ... dataFields) {
        return update(dataFields, 1);
    }

    public boolean update(int effectRow) {
        return update(null, effectRow);
    }

    public boolean update() {
        if(snapshot !=null) {
            return update(getModifyField());
        } else {
            return update(new String[0]);
        }
    }

    public boolean update(Consumer modifyFunction) {
        this.snapshot();
        modifyFunction.accept(this);
        return update();
    }

    public List<T> query(String[] dataFields, String[] andFields, Integer pageNum, Integer pageSize) {
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

    public <R> R customQuery(String dataSql, String[] andFields, Class<R> clazz) {
        Query query = Query.newInstance().and(andFields);
        return recorder.customQuery(null, dataSql, recorder.genWhereSql((T)this, query), (T)this, clazz);
    }

    public <R> R customQuery(String dataSql, Class<R> clazz) {
        if(snapshot !=null) {
            return customQuery(dataSql, getModifyField(), clazz);
        } else {
            return customQuery(dataSql, null, clazz);
        }
    }

    public boolean delete(String[] andFields, int effectRow) {
        Query query = Query.newInstance().and(andFields);

        try {
            if (recorder.delete((T)this, query) == effectRow) {
                recorder.getJdbcOperate().commit();
                return true;
            } else {
                recorder.getJdbcOperate().rollback();
                return false;
            }
        } catch (Exception e) {
            try {
                recorder.getJdbcOperate().rollback();
            } catch (SQLException throwables) {
                Logger.error("Dao.update exception rollback failed", e);
            }
            Logger.error("Dao.update failed", e);
            return false;
        }
    }

    public boolean delete(String ... andFields) {
        return delete(andFields, 1);
    }

    public boolean delete() {
        if(snapshot !=null) {
            return delete(getModifyField());
        } else {
            return delete(null);
        }
    }
}

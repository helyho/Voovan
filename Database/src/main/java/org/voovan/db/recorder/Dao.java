package org.voovan.db.recorder;

import org.voovan.db.JdbcOperate;
import org.voovan.db.recorder.annotation.NotInsert;
import org.voovan.db.recorder.annotation.NotUpdate;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.annotation.NotSerialization;

import javax.sql.DataSource;
import java.util.List;


/**
 * 类文字命名
 *
 * @author: helyho
 * walletbridge Framework.
 * WebSite: https://github.com/helyho/walletbridge
 * Licence: Apache v2 License
 */
public class Dao<T> {
    public Class<T> clazz;
    @NotUpdate
    @NotInsert
    @NotSerialization
    public transient javax.sql.DataSource dataSource;

    public Class<T> getClazz() {
        return clazz;
    }

    public void setClazz(Class<T> clazz) {
        this.clazz = clazz;
    }

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

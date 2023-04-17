package org.voovan.db.recorder;

import org.voovan.db.JdbcOperate;
import org.voovan.db.exception.DaoException;
import org.voovan.db.exception.UpdateFieldException;
import org.voovan.db.recorder.annotation.NotInsert;
import org.voovan.db.recorder.annotation.NotUpdate;
import org.voovan.db.recorder.exception.RecorderException;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.reflect.annotation.NotSerialization;

import javax.sql.DataSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Dao 对象基础类
 *
 * @author helyho
 * walletbridge Framework.
 * WebSite: https://github.com/helyho/walletbridge
 * Licence: Apache v2 License
 */
public class Dao<T extends Dao> {
    private static String[] EMPTY_STRING_ARRAY = new String[0];

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

    /**
     * 获取 JdbcOperate
     * @return JdbcOperate 对象
     */
    public JdbcOperate getJdbcOperate() {
        return jdbcOperate;
    }

    /**
     * 设置 JdbcOperate
     * @param jdbcOperate JdbcOperate 对象
     * @return  当前 Dao 对象
     */
    public T setJdbcOperate(JdbcOperate jdbcOperate) {
        if(jdbcOperate == null) {
            throw new RecorderException("JdbcOperate not null");
        }

        this.recorder = new Recorder(jdbcOperate);
        this.jdbcOperate = jdbcOperate;

        return (T) this;
    }

    /**
     * 设置数据源
     * @param dataSource 数据源对象
     * @return 返回 Dao 对象
     */
    public T setDatasource(DataSource dataSource) {
        this.recorder = new Recorder(dataSource);
        this.jdbcOperate = recorder.getJdbcOperate();

        return (T) this;
    }

    /**
     * 设置数据源
     * @param dataSource 数据源对象
     * @param isTrancation 是否事物模式
     * @return 返回 Dao 对象
     */
    public T setDatasource(DataSource dataSource, boolean isTrancation) {
        this.recorder = new Recorder(dataSource, isTrancation);
        this.jdbcOperate = recorder.getJdbcOperate();

        return (T) this;
    }

    /**
     * 当前 Dao 对象与参数Dao共享同一个数据库链接
     * @param dao Dao对象
     */
   public void share(Dao dao) {
        setJdbcOperate(dao.getJdbcOperate());
    }

    /**
     * 获取 Recorder 对象
     * @return Recorder 对象
     */
    public Recorder getRecorder() {
        return recorder;
    }

    /**
     * 设置 Recorder 对象
     * @param recorder Recorder 对象
     * @return  当前 Dao 对象
     */
    public T setRecorder(Recorder recorder) {
        if(recorder == null) {
            throw new RecorderException("Recorder must not null");
        }
        this.jdbcOperate = recorder.getJdbcOperate();
        this.recorder = recorder;

        return (T) this;
    }

    private void check() {
        if(jdbcOperate == null) {
            throw new DaoException("jdbcOperate is null");
        }
    }

    /**
     * 创建快照
     * @return 当前 Dao 对象
     */
    public T snapshot() {
        try {
            snapshot = TReflect.getMapFromObject(this, true);
            return (T)this;
        } catch (ReflectiveOperationException e) {
            throw new RecorderException("beginUpdate failed", e);
        }
    }

    /**
     * 和快照对比找出发生变更的对象属性
     * @return 发生变更的对象属性
     */
    private String[] getModifyField() {
        try {
            Map<String, Object> current = TReflect.getMapFromObject(this, true);

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
            return modifyField.toArray(EMPTY_STRING_ARRAY);
        } catch (ReflectiveOperationException e) {
            throw new RecorderException("endUpdate failed", e);
        }
    }

    /**
     * 插入数据
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean insert() {
        check();

        try {
            if (recorder.insert((T)this) == 1) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Logger.error("Dao.update failed", e);
            return false;
        }
    }

    /**
     * 使用主键更新指定的对象属性
     * @param dataFields 对象属性名称
     * @param andFields 更新记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @param effectRow  影响的行数, 小于 0 对影响行数无限制
     * @return 影响的行数于effectRow相等返回true, 否则返回false
     */
    public boolean update(String[] dataFields, String[] andFields, int effectRow) {
        check();
        try {
            boolean newTransaction = jdbcOperate.beginTransaction();
            Query query = Query.newInstance().data(dataFields).and(andFields);
            if (recorder.update((T) this, query) == effectRow || effectRow < 0) {
                if(newTransaction) {
                    jdbcOperate.commit();
                }
                return true;
            } else {
                jdbcOperate.rollback();
                return false;
            }
        } catch (Exception e) {
            try {
                jdbcOperate.rollback();
            } catch (SQLException throwables) {
                Logger.error("Dao.update rollback failed", e);
            }
            Logger.error("Dao.update failed", e);
            return false;
        }
    }

    /**
     * 使用主键更新指定的对象属性
     * @param dataFields 对象属性名称
     * @param effectRow  影响的行数, 小于 0 对影响行数无限制
     * @return 影响的行数于effectRow相等返回true, 否则返回false
     */
    public boolean update(String[] dataFields, int effectRow) {
        return update(dataFields, null, effectRow);
    }

    /**
     * 使用主键更新 dataFields 指定的所有属性, 但不包括主键
     * @param dataFields 指定需要更新的属性
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean update(String ... dataFields) {
        return update(dataFields, 1);
    }

    /**
     * 使用主键更新对象中所有非 null 的属性, 但不包括主键
     * @param effectRow 影响的行数, 小于 0 对影响行数无限制
     * @return 影响的行数于effectRow相等返回true, 否则返回false
     */
    public boolean update(int effectRow) {
        return update(null, effectRow);
    }

    /**
     * 如果有快照则使用主键根据对象变更的属性更新记录
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean update() {
        if(snapshot !=null) {
            String[]  updateFields = getModifyField();
            boolean ret = update(updateFields);

            try {
                if(!ret) {
                    rollbackInMemory();
                }
            } catch (Exception e) {
                throw new UpdateFieldException(e);
            }
            return ret;

        } else {
            return update(EMPTY_STRING_ARRAY);
        }
    }


    public void rollbackInMemory() throws ReflectiveOperationException {
        String[]  updateFields = getModifyField();
        //回滚内存对象
        for(String fieldName: updateFields) {

            java.lang.reflect.Field field = TReflect.findField(this.getClass(), fieldName);

            if (field != null) {
                TReflect.setFieldValue(this, fieldName, snapshot.get(fieldName));
            } else {
                throw new UpdateFieldException("Dao.updateField failed rollback " + fieldName + " failed");
            }
        }
    }

    /**
     * 使用主键更新的更新回调方法, 在 modifyFunction 中进行对象的属性更新
     * @param modifyFunction 对象属性更新 lambda
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean update(Consumer modifyFunction) {
        this.snapshot();
        modifyFunction.accept(this);
        return update();
    }


    /**
     * 使用主键更新指定的对象属性
     * @param fieldDatas 对象属性 {对象属性, 对象属性值}
     * @param effectRow  影响的行数
     * @return 影响的行数于effectRow相等返回true, 否则返回false
     */
    public boolean updateField(Map<String, Object> fieldDatas, int effectRow) {
        return updateField(fieldDatas, null, effectRow);
    }

    /**
     * 使用主键更新指定的对象属性
     * @param fieldDatas 对象属性 {对象属性, 对象属性值}
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean updateField(Map<String, Object> fieldDatas) {
        return updateField(fieldDatas, 1);
    }

    /**
     * 更新指定的对象属性
     * @param fieldDatas 对象属性 {对象属性, 对象属性值}
     * @param andFields 更新记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @param effectRow  影响的行数
     * @return 影响的行数于effectRow相等返回true, 否则返回false
     */
    public boolean updateField(Map<String, Object> fieldDatas, String[] andFields, int effectRow) {
        check();

        try {
            for(Map.Entry<String, Object> fieldData : fieldDatas.entrySet()) {
                String fieldName = fieldData.getKey();
                Object value = fieldData.getValue();

                java.lang.reflect.Field field = TReflect.findField(this.getClass(), fieldName);

                if (field != null) {
                    if (field.getAnnotation(NotUpdate.class) != null) {
                        throw new UpdateFieldException("Dao.updateField " + fieldName + " not for update by @NotUpdate");
                    }

                    TReflect.setFieldValue(this, fieldName, value);

                } else {
                    throw new UpdateFieldException("Dao.updateField " + fieldName + " not found");
                }
            }

            //调用通用的 update 方法
            update(fieldDatas.keySet().toArray(EMPTY_STRING_ARRAY), andFields, effectRow);
            return true;
        } catch (Exception e) {
            if(e instanceof UpdateFieldException) {
                throw (UpdateFieldException) e;
            }
            throw new UpdateFieldException(e);
        }
    }

    /**
     * 使用对象模型查询数据
     * @param dataFields select 和 from 之间作为数据返回的属性
     * @param andFields 查询记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @param pageNum  分页页号
     * @param pageSize 分页单页大小
     * @return 查询的结果集
     */
    public List<T> query(String[] dataFields, String[] andFields, Integer pageNum, Integer pageSize) {
        check();

        andFields = andFields == null && snapshot!=null ? getModifyField() : andFields;

        Query query = Query.newInstance().data(dataFields).and(andFields);
        query.page(pageNum, pageSize);

        try {

            List<T> ret = recorder.query((T)this, query);
            return ret;
        } catch (Exception e) {
            Logger.error("Dao.queryOne failed", e);
            return null;
        }

    }

    /**
     * 使用对象模型查询数据, 返回所有的属性
     * @param andFields 查询记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @param pageNum  分页页号
     * @param pageSize 分页单页大小
     * @return 查询的结果集
     */
    public List<T> query(String[] andFields, Integer pageNum, Integer pageSize) {
        return query(null, andFields, pageNum, pageSize);
    }

    /**
     * 使用对象模型查询数据
     * @param dataFields select 和 from 之间作为数据返回的属性
     * @param andFields 查询记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @return 查询的结果集
     */
    public List<T> query(String[] dataFields, String[] andFields) {
        return query(dataFields, andFields, null, null);
    }

    /**
     * 使用对象模型查询数据, 返回所有的属性
     * @param andFields 查询记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @return 查询的结果集
     */
    public List<T> query(String ... andFields) {
        return query(null, andFields, null, null);
    }

    /**
     * 使用对象模型查询数据,在 modifyFunction 中进行对象的属性进行查询, 返回所有的属性
     * @return 查询的结果集
     */
    public List<T> query() {
        return query(null, null, null, null);
    }


    /**
     * 使用自定义查询数据多条数据
     * @param dataSql select 和 from 之间的 sql 语句
     * @param whereSQL SQL 中 where 的查询条件
     * @param clazz 查询返回的对象类型
     * @param <R> 范型
     * @return 查询的结果
     */
    public <R> List<R> customQuery(String dataSql, String whereSQL, Class<R> clazz) {
        check();

        try {
            List<R> ret = recorder.customQuery(null, dataSql, whereSQL, (T)this, clazz);
            return ret;
        } catch (Exception e) {
            Logger.error("Dao.customQuery failed", e);
            return null;
        }
    }

    /**
     * 使用自定义查询数据多条数据
     * @param dataSql select 和 from 之间的 sql 语句
     * @param clazz 查询返回的对象类型
     * @param <R> 范型
     * @return 查询的结果
     */
    public <R> List<R> customQuery(String whereSQL, Class<R> clazz) {
        return customQuery(null, whereSQL, clazz);
    }

    /**
     * 使用对象模型查询一条数据
     * @param dataFields select 和 from 之间作为数据返回的属性
     * @param andFields 查询记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @return 查询的结果
     */
    public T queryOne(String[] dataFields, String[] andFields) {
        check();

        andFields = andFields == null && snapshot!=null ? getModifyField() : andFields;

        Query query = Query.newInstance().data(dataFields).and(andFields);
        try {

            List<T> results = recorder.query((T)this, query);

            T ret = null;

            if(results.size()==0) {
                ret = null;
            } else if(results.size() == 1) {
                ret = results.get(0);
            } else {
                Logger.warn("query return more than one result");
                ret = results.get(0);
            }

            return (T)ret;
        } catch (Exception e) {
            Logger.error("Dao.queryOne failed", e);
            return null;
        }
    }

    /**
     * 使用对象模型查询一条数据
     * @param andFields 查询记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @return 查询的结果
     */
    public T queryOne(String ... andFields) {
        return queryOne(null, andFields);
    }

    /**
     * 使用对象模型查询数据,在 modifyFunction 中进行对象的属性进行查询, 返回所有的属性
     * @return 查询的结果集
     */
    public T queryOne() {
        return queryOne(null, null);
    }

    /**
     * 使用自定义查询数据单条数据
     * @param dataSql select 和 from 之间的 sql 语句
     * @param andFields 查询记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @param clazz 查询返回的对象类型
     * @param <R> 范型
     * @return 查询的结果
     */
    public <R> R customQueryOne(String dataSql, String[] andFields, Class<R> clazz) {
        check();

        andFields = andFields == null && snapshot!=null ? getModifyField() : andFields;

        Query query = Query.newInstance().and(andFields);
        try {
            R ret = recorder.customQueryOne(null, dataSql, recorder.genWhereSql((T)this, query), (T)this, clazz);
            return ret;
        } catch (Exception e) {
            Logger.error("Dao.customQuery failed", e);
            return null;
        }
    }

    /**
     * 使用自定义查询数据单条数据
     * @param dataSql select 和 from 之间的 sql 语句
     * @param clazz 查询返回的对象类型
     * @param <R> 范型
     * @return 查询的结果
     */
    public <R> R customQueryOne(String dataSql, Class<R> clazz) {
        return customQueryOne(dataSql, null, clazz);
    }

    /**
     * 删除指定的数据
     * @param andFields 删除记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @param effectRow  影响的行数, 小于 0 对影响行数无限制
     * @return 影响的行数于effectRow相等返回true, 否则返回false
     */
    public boolean delete(String[] andFields, int effectRow) {
        check();

        andFields = andFields == null && snapshot!=null ? getModifyField() : andFields;

        Query query = Query.newInstance().and(andFields);

        try {
            boolean newTransaction = jdbcOperate.beginTransaction();
            if (recorder.delete((T)this, query) == effectRow || effectRow < 0) {
                if(newTransaction) {
                    jdbcOperate.commit();
                }
                return true;
            } else {
                jdbcOperate.rollback();
                return false;
            }
        } catch (Exception e) {
            try {
                jdbcOperate.rollback();
            } catch (SQLException throwables) {
                Logger.error("Dao.update rollback failed", e);
            }
            Logger.error("Dao.update failed", e);
            return false;
        }
    }

    /**
     * 删除指定的数据
     * @param andFields 删除记录的查询条件, 这些对像属性将使用 and 拼装出 where 后的条件
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean delete(String ... andFields) {
        check();

        return delete(andFields, 1);
    }

    /**
     * 删除指定的数据
     * @param effectRow  影响的行数, 小于 0 对影响行数无限制
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean delete(int effectRow) {
        check();

        return delete(null, effectRow);
    }

    /**
     * 如果有快照则使用主键根据对象变更的属性作为条件删除数据
     * @return 影响1行返回 true, 否则返回 false
     */
    public boolean delete() {
        check();
        return delete(null);
    }

    public T lock() {
        Query query = Query.newInstance().custom(" for update");
        return (T)recorder.queryOne(this, query);
    }

    /**
     * 事物模式
     *   如果事物失败,所有daos 中的对象属性将会被回滚
     * @param transLogic 事物逻辑
     * @param daos 所以加入事物的 Dao 对象
     * @return 事物逻辑的返回值
     * @param <T> 返回值的泛型
     * @throws Exception 异常
     */
    public <T> T transaction(Supplier<T> transLogic, Dao...daos) throws Exception {
        for(Dao dao: daos) {
            dao.setJdbcOperate(jdbcOperate);
            dao.snapshot();
        }

        T ret;

        try {
            ret = (T) transLogic.get();
        } catch(Throwable e) {
            for(Dao dao: daos) {
                dao.rollbackInMemory();
            }
            jdbcOperate.rollback();
            throw e;
        }
        jdbcOperate.commit();

        return ret;
    }

    /**
     * 事物模式
     *   如果事物失败,所有daos 中的对象属性将会被回滚
     * @param dataSource 数据源
     * @param transLogic 事物逻辑
     * @param daos 所以加入事物的 Dao 对象
     * @return 事物逻辑的返回值
     * @param <T> 返回值的泛型
     * @throws Exception 异常
     */
    public static <T> T transaction(DataSource dataSource, Supplier<T> transLogic, Dao...daos) throws Exception {
        Dao root = new Dao();
        root.setDatasource(dataSource, true);
        return (T)root.transaction(transLogic, daos);
    }

    /**
     * 事物模式
     *   如果事物失败,所有daos 中的对象属性将会被回滚
     * @param jdbcOperate JdbcOperate 对象
     * @param transLogic 事物逻辑
     * @param daos 所以加入事物的 Dao 对象
     * @return 事物逻辑的返回值
     * @param <T> 返回值的泛型
     * @throws Exception 异常
     */
    public static <T> T transaction(JdbcOperate jdbcOperate, Supplier<T> transLogic, Dao...daos) throws Exception {
        Dao root = new Dao();
        root.setJdbcOperate(jdbcOperate);
        return (T)root.transaction(transLogic, daos);
    }

    /**
     * 事物模式
     *   如果事物失败,所有daos 中的对象属性将会被回滚
     * @param dataSource 数据源
     * @param transLogic 事物逻辑
     * @param daos 所以加入事物的 Dao 对象
     * @return 事物逻辑的返回值
     * @param <T> 返回值的泛型
     * @throws Exception 异常
     */
    public static <T> T transaction(DataSource dataSource, Runnable transLogic, Dao...daos) throws Exception {
        Dao root = new Dao();
        root.setDatasource(dataSource, true);
        return (T)root.transaction(()->{
            transLogic.run();
            return null;
        }, daos);
    }

    /**
     * 事物模式
     *   如果事物失败,所有daos 中的对象属性将会被回滚
     * @param jdbcOperate JdbcOperate 对象
     * @param transLogic 事物逻辑
     * @param daos 所以加入事物的 Dao 对象
     * @return 事物逻辑的返回值
     * @param <T> 返回值的泛型
     * @throws Exception 异常
     */
    public static <T> T transaction(JdbcOperate jdbcOperate, Runnable transLogic, Dao...daos) throws Exception {
        Dao root = new Dao();
        root.setJdbcOperate(jdbcOperate);
        return (T)root.transaction(()->{
            transLogic.run();
            return null;
        }, daos);
    }

    public T forQuery() {
        Field[] fields = TReflect.getFields(this.getClass());

        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }

            try {
				TReflect.setFieldValue(this, field.getName(), null);
			} catch (ReflectiveOperationException e) {
                Logger.errorf("{}.forQuery failed", e, this.getClass().getSimpleName());
			}
        }

        return (T) this;
    } 
}

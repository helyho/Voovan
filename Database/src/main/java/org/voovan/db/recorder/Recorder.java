package org.voovan.db.recorder;

import org.voovan.db.DataBaseType;
import org.voovan.db.JdbcOperate;
import org.voovan.db.recorder.annotation.NotInsert;
import org.voovan.db.recorder.annotation.NotUpdate;
import org.voovan.db.recorder.annotation.PrimaryKey;
import org.voovan.db.recorder.annotation.Table;
import org.voovan.db.recorder.exception.RecorderException;
import org.voovan.tools.TObject;
import org.voovan.tools.TSQL;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库记录操作类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class Recorder {
    public Map<String, String> SQL_TEMPLATE_CACHE = new ConcurrentHashMap<String, String>();

    public static String getCacheKey(String type, String mark, Class clazz, Query query) {
        return type + (mark==null? "" : "_" + mark.hashCode()) + "_" + clazz.hashCode() + (query==null ? "" : "_" + query.hashCode());
    }

    private JdbcOperate jdbcOperate;
    private boolean camelToUnderline = true;

    /**
     * 构造函数
     * @param jdbcOperate DBAccess 数据库连接对象
     */
    public Recorder(JdbcOperate jdbcOperate){
        this.jdbcOperate = jdbcOperate;
        this.camelToUnderline = true;
    }


    /**
     * 构造函数
     * @param jdbcOperate DBAccess 数据库连接对象
     * @param camelToUnderline 是否将驼峰转换为下划线形式
     */
    public Recorder(JdbcOperate jdbcOperate, boolean camelToUnderline){
        this.jdbcOperate = jdbcOperate;
        this.camelToUnderline = camelToUnderline;
    }

    public JdbcOperate getJdbcOperate() {
        return jdbcOperate;
    }

    public void setJdbcOperate(JdbcOperate jdbcOperate) {
        this.jdbcOperate = jdbcOperate;
    }

    /**
     * 查询操作
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 查询结果
     */
    public <T> List<T> query(String tableName, T obj, Query query) {
        try {
            return (List<T>) jdbcOperate.queryObjectList(buildQuerySqlTemplate(tableName, obj, query), obj.getClass(), obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder query error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 查询操作
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 查询结果
     */
    public <T> List<T> query(T obj, Query query) {
        return query(null, obj, query);
    }

    /**
     * 查询操作
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 查询结果
     */
    public <T> List<T> query(String tableName, T obj) {
        return query(tableName, obj, null);
    }

    /**
     * 查询操作
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 查询结果
     */
    public <T> List<T> query(T obj) {
        return query(null, obj, null);
    }

    /**
     * 自定义查询操作
     * @param tableName 指定的表名
     * @param dataSql select 和 from 之间的 sql 片段
     * @param whereSql where 后的 sql 片段
     * @param obj 数据 ORM 对象
     * @param clazz 返回类型
     * @param <T> 范型类型
     * @param <R> 返回值的范型类型
     * @return 累计数据条数
     */
    public <T, R> R customQuery(String tableName, String dataSql, String whereSql, T obj, Class<R> clazz) {
        try {
            whereSql = whereSql == null ? "" : whereSql;

            Table table = obj.getClass().getAnnotation(Table.class);

            if (tableName == null) {
                tableName = getTableNameWithDataBase(obj);
            }

            String sqlStr = TString.assembly("select ", dataSql, " from ", tableName, " ", whereSql);
            return jdbcOperate.queryObject(sqlStr, clazz,  obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder statistics error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 更新操作
     *      如果 Query 中的 data.length == 0 则更新所有非 null 的属性
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int update(String tableName, T obj, Query query) {
        try {
            return jdbcOperate.update(buildUpdateSqlTemplate(tableName, obj, query), obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder update error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 更新操作
     *      如果 Query 中的 data.length == 0, 更新对象中所有非 null 的属性, 但不包括主键
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int update(T obj, Query query) {
        return update(null, obj, query);
    }

    /**
     * 更新操作
     *      更新对象中所有非 null 的属性, 但不包括主键
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int update(String tableName, T obj) {
        return update(tableName, obj, null);
    }

    /**
     * 更新操作
     *      更新对象中所有非 null 的属性, 但不包括主键
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int update(T obj) {
        return update(null, obj, null);
    }

    /**
     * 删除操作
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int delete(String tableName, T obj, Query query) {
        try {
            return jdbcOperate.update(buildDeleteSqlTemplate(tableName, obj, query), obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder delete error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 删除操作
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int delete(T obj, Query query) {
        return delete(null, obj, query);
    }

    /**
     * 删除操作
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int delete(String tableName, T obj) {
        return delete(tableName, obj, null);
    }

    /**
     * 删除操作
     * @param obj 数据 ORM 对象
     * @return 更新数据条数
     * @param <T> 范型类型
     */
    public <T> int delete(T obj) {
        return delete(null, obj, null);
    }


    /**
     * 插入操作
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int insert(String tableName, T obj) {
        try{
            return jdbcOperate.update(buildInsertSqlTemplate(tableName, obj), obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder insert error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 插入操作
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 更新数据条数
     */
    public <T> int insert(T obj) {
        return insert(null, obj);
    }

    public <T> String buildQueryField(T obj, Query query) {
        String mainSql = "";
        if(query==null || query.getDataFields().size()==0){
            mainSql = mainSql + "*";
        } else {
            for (String resultField : query.getDataFields()) {
                if(TReflect.findFieldIgnoreCase(obj.getClass(), resultField)!=null) {
                    mainSql = mainSql + TSQL.wrapSqlField(jdbcOperate, TString.camelToUnderline(resultField)) + ",";
                }
            }
        }

        if(mainSql.endsWith(",")) {
            mainSql = TString.removeSuffix(mainSql);
        }

        return mainSql;
    }

    /**
     * 构造查询的 SQL
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 拼装的 SQL
     */
    public <T> String buildQuerySqlTemplate(String tableName, T obj, Query query) {
        String cacheKey = getCacheKey("QUERY", tableName, obj.getClass(), query);
        String resultSql = SQL_TEMPLATE_CACHE.get(cacheKey);

        if(resultSql == null) {
            Table table = obj.getClass().getAnnotation(Table.class);

            if (tableName == null) {
                tableName = getTableNameWithDataBase(obj);
            }

            //SQL模板准备
            //准备查询列
            String mainSql = "select " + buildQueryField(obj, query);

            mainSql = TString.assembly(mainSql, " from ", tableName);

            //处理查询条件
            String whereSql = genWhereSql(obj, query);

            //处理排序
            String orderSql = "order by ";

            if (query != null) {
                for (Map.Entry<String[], Boolean> entry : query.getOrderFields().entrySet()) {
                    for (String orderField : entry.getKey()) {
                        if (TReflect.findFieldIgnoreCase(obj.getClass(), orderField) != null) {
                            orderSql = orderSql + orderField + ",";
                        }
                    }

                    if (orderSql.endsWith(",")) {
                        orderSql = TString.removeSuffix(orderSql);
                    }

                    orderSql = orderSql + (entry.getValue() ? " desc" : " asc") + ",";
                }
            }

            if (orderSql.equals("order by ")) {
                orderSql = "";
            }

            if (orderSql.endsWith(",")) {
                orderSql = TString.removeSuffix(orderSql);
            }

            resultSql = TString.assembly(mainSql, " ", whereSql, " ", orderSql);

            //自动识别数据库类型选择不同的方言进行分页
            if (query != null) {
                DataBaseType dataBaseType = jdbcOperate.getDataBaseType();
                if (dataBaseType.equals(DataBaseType.Mariadb) || dataBaseType.equals(DataBaseType.MySql)) {
                    resultSql = genMysqlPageSql(resultSql, query);
                } else if (dataBaseType.equals(DataBaseType.Oracle)) {
                    resultSql = genOraclePageSql(resultSql, query);
                } else if (dataBaseType.equals(DataBaseType.Postage)) {
                    resultSql = genPostgrePageSql(resultSql, query);
                }
            }

            SQL_TEMPLATE_CACHE.put(cacheKey, resultSql);
        }

        return resultSql;
    }

    /**
     * 构造更新的 SQL
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param query 查询条件
     * @param <T> 范型类型
     * @return 拼装的 SQL
     */
    public <T> String buildUpdateSqlTemplate(String tableName, T obj, Query query) {
        String cacheKey = getCacheKey("UPDATE", tableName, obj.getClass(), query);
        String resultSql = SQL_TEMPLATE_CACHE.get(cacheKey);

        if(resultSql == null) {
            Table table = obj.getClass().getAnnotation(Table.class);

            if(tableName == null){
                tableName = getTableNameWithDataBase(obj);
            }

            //SQL模板准备
            String mainSql = TString.assembly("update " , tableName , " set");
            String setSql = "";

            //Set拼接 sql
            Field[] fields = TReflect.getFields(obj.getClass());

            for(Field field : fields){
                if(Modifier.isStatic(field.getModifiers())){
                    continue;
                }

                String sqlFieldName = getSqlFieldName(jdbcOperate, field);
                String fieldName = field.getName();

                try {
                    Object fieldValue = TReflect.getFieldValue(obj, fieldName);

                    //如果指定了列则不做以下判断
                    if(query==null || query.getDataFields().size() == 0) {
                        //检查字段是否为空
                        if (fieldValue == null) {
                            continue;
                        }

                        //主键不更新, 如果指定了列, 则不做主键的判断
                        if (field.getAnnotation(PrimaryKey.class) != null) {
                            continue;
                        }

                        //NotUpdate 注解不更新
                        NotUpdate notUpdate = field.getAnnotation(NotUpdate.class);
                        if (notUpdate != null && (notUpdate.value().equals("ANY_VALUE") || notUpdate.value().equals(fieldValue.toString()))) {
                            continue;
                        }
                    } else {
                        //如果有指定更新的列,则使用指定更新的列
                        if (query != null && query.getDataFields().size() > 0 && !query.getDataFields().contains(fieldName)) {
                            continue;
                        }
                    }

                } catch (ReflectiveOperationException e) {
                    Logger.error(e);
                }

                setSql = TString.assembly(setSql, sqlFieldName, "=::", fieldName, ",");
            }

            if(setSql.endsWith(",")){
                setSql = TString.removeSuffix(setSql);
            }

            //Where 拼接 sql
            String whereSql = genWhereSql(obj, query);

            if (whereSql.trim().equals("where 1=1")) {
                throw new RecorderException("Where sql must be have some condiction");
            }

            resultSql = TString.assembly(mainSql, " ", setSql, " ", whereSql);
            SQL_TEMPLATE_CACHE.put(cacheKey, resultSql);
        }


        return resultSql;
    }

    /**
     * 构造插入的 SQL
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 拼装的 SQL
     */
    public <T> String buildInsertSqlTemplate(String tableName, T obj) {
        String cacheKey = getCacheKey("INSERT", tableName, obj.getClass(), null);
        String resultSql = SQL_TEMPLATE_CACHE.get(cacheKey);

        if(resultSql == null) {

            Table table = obj.getClass().getAnnotation(Table.class);

            if (tableName == null) {
                tableName = getTableNameWithDataBase(obj);
            }

            //SQL模板准备
            String mainSql = TString.assembly("insert into ", tableName);
            String fieldSql = "";
            String fieldValueSql = "";

            //字段拼接 sql
            Field[] fields = TReflect.getFields(obj.getClass());

            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                String sqlFieldName = getSqlFieldName(jdbcOperate, field);
                String fieldName = field.getName();

                //如果主键为空则不插入主键字段
                if (field.getAnnotation(NotInsert.class) == null) {
                    try {
                        Object fieldValue = TReflect.getFieldValue(obj, fieldName);
                        if (fieldValue == null) {
                            continue;
                        }
                    } catch (ReflectiveOperationException e) {
                        Logger.error(e);
                    }
                } else {
                    continue;
                }

                fieldSql = fieldSql + sqlFieldName + ",";
                fieldValueSql = TString.assembly(fieldValueSql, "::", fieldName, ",");
            }

            resultSql = TString.assembly(mainSql, " (", TString.removeSuffix(fieldSql), ") ", "values (", TString.removeSuffix(fieldValueSql), ") ");
            SQL_TEMPLATE_CACHE.put(cacheKey, resultSql);
        }

        return resultSql;
    }

    /**
     * 构造删除的 SQL
     * @param tableName 指定的表名
     * @param obj 数据 ORM 对象
     * @param query 查询对象
     * @param <T> 范型类型
     * @return 拼装的 SQL
     */
    public <T> String buildDeleteSqlTemplate(String tableName, T obj, Query query) {
        String cacheKey = getCacheKey("DELETE", tableName, obj.getClass(), query);
        String resultSql = SQL_TEMPLATE_CACHE.get(cacheKey);

        if(resultSql == null) {

            Table table = obj.getClass().getAnnotation(Table.class);

            if (tableName == null) {
                tableName = getTableNameWithDataBase(obj);
            }

            //SQL模板准备
            String mainSql = TString.assembly("delete from ", tableName);

            //Where 拼接 sql
            String whereSql = genWhereSql(obj, query);

            resultSql = TString.assembly(mainSql, " ", whereSql);
            SQL_TEMPLATE_CACHE.put(cacheKey, resultSql);
        }

        return resultSql;
    }


    /**
     * 生成 where 后面的 sql
     * @param obj  数据对象
     * @param query 查询对象
     * @return where 后面的 sql
     */
    public String genWhereSql(Object obj, Query query) {
        String cacheKey = getCacheKey("WHERE", null, obj.getClass(), query);
        String whereSql = SQL_TEMPLATE_CACHE.get(cacheKey);

        if(whereSql == null) {

            whereSql = "where 1=1";

            if (
                    query == null || (
                                    !query.hasCondiction() &&
                                    query.getPageNumber() <= 0 && query.getPageSize() <=0 &&
                                    query.getOrderFields().isEmpty())
            ) {
                Field[] fields = TReflect.getFields(obj.getClass());
                //字段拼接 sql
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    //如果没有指定查询条件,则使用主键作为条件
                    if (field.getAnnotation(PrimaryKey.class) != null) {
                        String sqlFieldName = getSqlFieldName(jdbcOperate, field);
                        String fieldName = field.getName();

                        whereSql = TString.assembly(whereSql, " and ", sqlFieldName, " =::", fieldName);
                        break;
                    }
                }
            } else {

                for (Map.Entry<String, Query.Operate> entry : query.getAndFields().entrySet()) {
                    if (TReflect.findFieldIgnoreCase(obj.getClass(), entry.getKey()) != null) {
                        String sqlField = entry.getKey();
                        if (camelToUnderline) {
                            sqlField = TString.camelToUnderline(sqlField);
                        }
                        whereSql = TString.assembly(whereSql, " and ", sqlField, Query.getActualOperate(entry.getValue()), "::", entry.getKey());
                    }
                }

                for (Map.Entry<String, Query.Operate> entry : query.getOrFields().entrySet()) {
                    if (TReflect.findFieldIgnoreCase(obj.getClass(), entry.getKey()) != null) {
                        String sqlField = entry.getKey();
                        if (camelToUnderline) {
                            sqlField = TString.camelToUnderline(sqlField);
                        }

                        whereSql = TString.assembly(whereSql, " or ", sqlField, Query.getActualOperate(entry.getValue()), "::", entry.getKey());
                    }
                }

                if (whereSql.endsWith("or ") || whereSql.endsWith("and ")) {
                    int index = whereSql.trim().lastIndexOf(" ");
                    whereSql = whereSql.substring(0, index);
                }

                for (String customCondiction : query.getCustomCondictions()) {
                    whereSql = TString.assembly(whereSql, " ", customCondiction);
                }
            }

            SQL_TEMPLATE_CACHE.put(cacheKey, whereSql);
        }

        return whereSql;
    }

    /**
     * 生成 Mysql 分页的 sql
     * @param sql sql 字符串
     * @param query 查询对象
     * @return 处理后的 sql 字符串
     */
    public String genMysqlPageSql(String sql, Query query){
        if(query.getPageSize()<0 || query.getPageNumber()<0) {
            return sql;
        }

        return TSQL.genMysqlPageSql(sql, query.getPageNumber(), query.getPageSize());
    }

    /**
     * 生成 Postage 分页的 sql
     * @param sql sql 字符串
     * @param query 查询对象
     * @return  处理后的 sql 字符串
     */
    public String genPostgrePageSql(String sql, Query query){
        if(query.getPageSize()<0 || query.getPageNumber()<0) {
            return sql;
        }

        return TSQL.genPostgrePageSql(sql, query.getPageNumber(), query.getPageSize());
    }

    /**
     * 生成Oracle 分页的 sql
     * @param sql sql 字符串
     * @param query 查询对象
     * @return  处理后的 sql 字符串
     */
    public String genOraclePageSql(String sql, Query query){
        if(query.getPageSize()<0 || query.getPageNumber()<0) {
            return sql;
        }

        return TSQL.genOraclePageSql(sql, query.getPageNumber(), query.getPageSize());
    }


    /**
     * 获取数据库名
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 数据库名
     */
    public <T> String getSqlDatabase(T obj){
        Table tableAnnotation = obj.getClass().getAnnotation(Table.class);

        if(tableAnnotation==null){
            return "";
        }

        //处理数据库名
        String databaseName = (tableAnnotation.database().equals("") ? "" : tableAnnotation.database() + ".");
        return databaseName;
    }

    /**
     * 获取表名
     * @param obj 数据 ORM 对象
     * @param <T> 范型类型
     * @return 表名
     */
    public <T> String getSqlTableName(T obj){
        Table tableAnnotation = obj.getClass().getAnnotation(Table.class);

        //处理表名
        String tableName = "";
        if(tableAnnotation==null){
            tableName = obj.getClass().getSimpleName();
        } else if(!tableAnnotation.name().equals("")){
            tableName = tableAnnotation.name();
        } else if(!tableAnnotation.value().equals("")){
            tableName = tableAnnotation.value();
        } else {
            tableName = obj.getClass().getSimpleName();
        }

        if(camelToUnderline) {
            tableName = TString.camelToUnderline(tableName);
        }

        if(tableAnnotation!=null) {
            if (tableAnnotation.lowerCase()) {
                tableName = tableName.toLowerCase();
            } else if (tableAnnotation.upperCase()) {
                tableName = tableName.toUpperCase();
            } else if (tableAnnotation.upperCaseHead()) {
                tableName = TString.upperCaseHead(tableName);
            }
        }

        return tableName;
    }

    /**
     * 获取带有数据库名的表名字符串
     * @param obj 对象
     * @return 带有数据库名的表名字符串
     */
    public String getTableNameWithDataBase(Object obj){
        //处理数据库名
        String databaseName = getSqlDatabase(obj);

        //处理表名
        String tableName = getSqlTableName(obj);

        return databaseName + tableName;
    }

    /**
     * 获取字段名
     * @param jdbcOperate JdbcOperate 对象
     * @param field 字段对象
     * @return 字段名
     */
    public String getSqlFieldName(JdbcOperate jdbcOperate, Field field) {
        String fieldName = "";

        org.voovan.db.recorder.annotation.Field fieldAnnotation = field.getAnnotation(org.voovan.db.recorder.annotation.Field.class);

        if(fieldAnnotation==null){
            fieldName = field.getName();
        } else if(!fieldAnnotation.name().equals("")){
            fieldName = fieldAnnotation.name();
        } else if(!fieldAnnotation.value().equals("")){
            fieldName = fieldAnnotation.value();
        } else {
            fieldName = field.getName();
        }

        if(camelToUnderline) {
            fieldName = TString.camelToUnderline(fieldName);
        }

        if(fieldAnnotation!=null){
            if (fieldAnnotation.lowerCase()) {
                fieldName = fieldName.toLowerCase();
            } else if (fieldAnnotation.upperCase()) {
                fieldName = fieldName.toUpperCase();
            } else if (fieldAnnotation.upperCaseHead()) {
                fieldName = TString.upperCaseHead(fieldName);
            }
        }

        return TSQL.wrapSqlField(jdbcOperate, fieldName);
    }

    /**
     * 根据需要更新的字段构造一个更新对象
     * @param data  对象
     * @param updateFilds 更新的字段
     * @param <R> 对象
     * @return 可用户更新的对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static <R> R buildUpdateBaseObject(R data, String ... updateFilds) throws ReflectiveOperationException {
        List<String> updateFieldList = TObject.asList(updateFilds);
        Field[] fields = TReflect.getFields(data.getClass());

        for(Field field : fields){
            if(Modifier.isStatic(field.getModifiers())){
                continue;
            }

            //主键不更新
            if(field.getAnnotation(PrimaryKey.class)==null && !updateFieldList.contains(field.getName())) {
                TReflect.setFieldValue(data, field.getName(), null);
            }
        }

        return null;
    }


}

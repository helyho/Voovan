package org.voovan.db.recorder;

import org.voovan.db.recorder.annotation.PrimaryKey;
import org.voovan.db.recorder.annotation.Table;
import org.voovan.db.recorder.exception.RecorderException;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.TReflect;
import org.voovan.db.JdbcOperate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 数据库记录操作类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class Recorder {

    private JdbcOperate dbAccess;

    /**
     * 构造函数
     * @param dbAccess DBAccess 数据库连接对象
     */
    public Recorder(JdbcOperate dbAccess){
        this.dbAccess = dbAccess;
    }

    /**
     * 更新操作
     * @param obj 数据 ORM 对象
     * @return 更新数据条数
     * @throws RecorderException Recorder 操作异常
     */
    public <T> List<T> query(T obj, Query query) throws RecorderException {
        try {
            return (List<T>)dbAccess.queryObjectList(buildQuerySqlTemplate(obj, query), obj.getClass(), obj);
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
     * @param obj 数据 ORM 对象
     * @return 更新数据条数
     * @throws RecorderException Recorder 操作异常
     */
    public <T> int update(T obj) throws RecorderException {
        try {
            return dbAccess.update(buildUpdateSqlTemplate(obj), obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder update error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 插入操作
     * @param obj 数据 ORM 对象
     * @return 更新数据条数
     * @throws RecorderException Recorder 操作异常
     */
    public <T> int insert(T obj) throws RecorderException {
        try{
            return dbAccess.update(buildInsertSqlTemplate(obj), obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder update error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 删除操作
     * @param obj 数据 ORM 对象
     * @return 更新数据条数
     * @throws RecorderException Recorder 操作异常
     */
    public <T> int delete(T obj) throws RecorderException {
        try {
            return dbAccess.update(buildDeleteSqlTemplate(obj), obj);
        }catch (Exception e){
            if(e instanceof RecorderException){
                throw (RecorderException)e;
            } else {
                throw new RecorderException("Recorder update error: " + JSON.toJSON(obj), e);
            }
        }
    }

    /**
     * 构造查询的 SQL
     * @param obj 数据 ORM 对象
     * @return 拼装的 SQL
     * @throws RecorderException Recorder 操作异常
     */
    public <T> String buildQuerySqlTemplate(T obj, Query query) throws RecorderException {
        Table table = obj.getClass().getAnnotation(Table.class);

        //处理数据库名
        String databaseName = getSqlDatabase(obj);

        //处理表名
        String tableName = getSqlTableName(obj);

        //SQL模板准备
        //准备查询列
        String mainSql = "select ";
        if(query.getResultField().size()==0){
            mainSql = mainSql + "*";
        } else {
            for (String resultField : query.getResultField()) {
                mainSql = mainSql + resultField + ",";
            }
        }

        if(mainSql.endsWith(",")) {
            mainSql = TString.removeSuffix(mainSql);
        }

        mainSql = mainSql + " from " + databaseName + tableName;

        //处理查询条件
        String whereSql = "where 1=1";
        for(Map.Entry<String, Query.Operate> entry : query.getQueryAndField().entrySet()) {
            try {
                if(TReflect.findFieldIgnoreCase(obj.getClass(), entry.getKey())!=null) {
                    whereSql = whereSql + " and " + TString.camelToUnderline(entry.getKey()) + Query.getActualOperate(entry.getValue()) + "=::" + entry.getKey();
                }
            } catch (ReflectiveOperationException e) {
                throw new RecorderException("Recorder query result field is failed", e);
            }
        }

        for(Map.Entry<String, Query.Operate> entry : query.getQueryOrField().entrySet()) {
            try{
                if(TReflect.findFieldIgnoreCase(obj.getClass(), entry.getKey())!=null) {
                    whereSql = whereSql + " or " + TString.camelToUnderline(entry.getKey()) + Query.getActualOperate(entry.getValue()) + "=::" + entry.getKey();
                }
            } catch (ReflectiveOperationException e) {
                throw new RecorderException("Recorder query result field is failed", e);
            }
        }

        if(whereSql.endsWith("or ") || whereSql.endsWith("and ")){
            int index = whereSql.trim().lastIndexOf(" ");
            whereSql = whereSql.substring(0, index);
        }

        //处理排序
        String orderSql = "order by ";

        for(Map.Entry<String[], Boolean> entry : query.getOrderField().entrySet()) {
            for(String orderField : entry.getKey()) {
                orderSql = orderSql + orderField + ",";
            }

            if(orderSql.endsWith(",")) {
                orderSql = TString.removeSuffix(orderSql);
            }

            orderSql = orderSql + (entry.getValue() ? " desc" : " asc") + ",";
        }

        if(orderSql.endsWith(",")) {
            orderSql = TString.removeSuffix(orderSql);
        }

        String resultSql = mainSql + " " + whereSql + " "  + orderSql;

        return resultSql;
    }


    /**
     * 构造更新的 SQL
     * @param obj 数据 ORM 对象
     * @return 拼装的 SQL
     * @throws RecorderException Recorder 操作异常
     */
    public <T> String buildUpdateSqlTemplate(T obj) throws RecorderException {
        Table table = obj.getClass().getAnnotation(Table.class);

        //处理数据库名
        String databaseName = getSqlDatabase(obj);

        //处理表名
        String tableName = getSqlTableName(obj);

        //SQL模板准备
        String mainSql = "update " + databaseName + tableName + " set";
        String setSql = "";
        String whereSql = "";

        //字段拼接 sql
        Field[] fields = TReflect.getFields(obj.getClass());
        for(Field field : fields){

            String sqlFieldName = getSqlFieldName(field);
            String fieldName = field.getName();

            if(field.getAnnotation(PrimaryKey.class) != null) {
                whereSql = "where " + sqlFieldName + "=::" + fieldName;
            } else {
                setSql = setSql + sqlFieldName + "=::" + fieldName + ",";
            }
        }

        if(whereSql.equals("")){
            throw new RecorderException("Recorder primaryKey annotation is not set");
        }

        if(setSql.endsWith(",")){
            setSql = TString.removeSuffix(setSql);
        }

        String resultSql = mainSql + " " + setSql + " " + whereSql;

        return resultSql;
    }

    /**
     * 构造插入的 SQL
     * @param obj 数据 ORM 对象
     * @return 拼装的 SQL
     * @throws RecorderException Recorder 操作异常
     */
    public <T> String buildInsertSqlTemplate(T obj) throws RecorderException {
        Table table = obj.getClass().getAnnotation(Table.class);

        //处理数据库名
        String databaseName = getSqlDatabase(obj);

        //处理表名
        String tableName = getSqlTableName(obj);

        //SQL模板准备
        String mainSql = "insert into " + databaseName + tableName;
        String fieldSql = "";
        String fieldValueSql = "";

        //字段拼接 sql
        Field[] fields = TReflect.getFields(obj.getClass());
        for(Field field : fields){

            String sqlFieldName = getSqlFieldName(field);
            String fieldName = field.getName();

            fieldSql = fieldSql + sqlFieldName + ",";
            fieldValueSql = fieldValueSql + "::" + fieldName + ",";
        }

        String resultSql = mainSql + " ("+ TString.removeSuffix(fieldSql) + ") " + "values ("+ TString.removeSuffix(fieldValueSql) + ") ";

        return resultSql;
    }

    /**
     * 构造删除的 SQL
     * @param obj 数据 ORM 对象
     * @return 拼装的 SQL
     * @throws RecorderException
     */
    public <T> String buildDeleteSqlTemplate(T obj) throws RecorderException {
        Table table = obj.getClass().getAnnotation(Table.class);

        //处理数据库名
        String databaseName = getSqlDatabase(obj);

        //处理表名
        String tableName = getSqlTableName(obj);

        //SQL模板准备
        String mainSql = "delete from " + databaseName + tableName;
        String whereSql = "";

        //字段拼接 sql
        Field[] fields = TReflect.getFields(obj.getClass());
        for(Field field : fields){

            String sqlFieldName = getSqlFieldName(field);
            String fieldName = field.getName();

            if(field.getAnnotation(PrimaryKey.class) != null) {
                whereSql = "where " + sqlFieldName + " =::" + fieldName;
                break;
            }
        }

        if(whereSql.equals("")){
            throw new RecorderException("Recorder primaryKey annotation is not set");
        }

        String resultSql = mainSql + " " + whereSql;

        return resultSql;
    }

    /**
     * 获取数据库名
     * @param obj 数据 ORM 对象
     * @return 数据库名
     */
    public static <T> String getSqlDatabase(T obj){
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
     * @return 表名
     */
    public static <T> String getSqlTableName(T obj){
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

        tableName = TString.camelToUnderline(tableName);

        if(tableAnnotation!=null) {
            if (tableAnnotation.lowerCase() == 1) {
                tableName = tableName.toLowerCase();
            } else if (tableAnnotation.upperCase() == 1) {
                tableName = tableName.toUpperCase();
            }
        }

        return tableName;
    }

    /**
     * 获取字段名
     * @param field 字段对象
     * @return 字段名
     */
    public static String getSqlFieldName(Field field) {
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

        fieldName = TString.camelToUnderline(fieldName);

        if(fieldAnnotation!=null){
            if (fieldAnnotation.lowerCase() == 1) {
                fieldName = fieldName.toLowerCase();
            } else if (fieldAnnotation.upperCase() == 1) {
                fieldName = fieldName.toUpperCase();
            }
        }

        return fieldName;
    }

}

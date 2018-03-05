package org.voovan.db;

import org.voovan.tools.TSQL;
import org.voovan.tools.log.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 结果集和数据库连接封装
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ResultInfo {
    private ResultSet resultSet;
    private JdbcOperate jdbcOperate;

    public ResultInfo(ResultSet resultSet,JdbcOperate jdbcOperate) {
        this.resultSet = resultSet;
        this.jdbcOperate =jdbcOperate;
    }

    public JdbcOperate getJdbcOperate() {
        return jdbcOperate;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getObjectList(Class<T> t) {
        try{
            return (List<T>) TSQL.getAllRowWithObjectList(t, this.resultSet);
        }catch(SQLException | ReflectiveOperationException | ParseException e){
            Logger.error("JdbcOperate.getObjectList error",e);
        }finally{
            // 非事务模式执行
            if (jdbcOperate.getTranscationType() == TranscationType.NONE) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return new ArrayList<T>();

    }

    public List<Map<String, Object>> getMapList() {
        try{
            return TSQL.getAllRowWithMapList(this.resultSet);
        }catch(SQLException | ReflectiveOperationException e){
            Logger.error("JdbcOperate.getMapList error",e);
        }finally{
            // 非事务模式执行
            if (jdbcOperate.getTranscationType() == TranscationType.NONE) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return new ArrayList<Map<String, Object>>();
    }

    @SuppressWarnings("unchecked")
    public <T> Object getObject(Class<T> t){
        try{
            if(resultSet.next()){
                Object obj = TSQL.getOneRowWithObject(t, this.resultSet);
                if(obj instanceof Map){
                    Map map = (Map)obj;
                    if(map.size() > 0){
                        obj = map.values().iterator().next().toString();
                    } else {
                        obj = null;
                    }
                }
                return (T)obj;
            }else{
                return null;
            }
        }catch(SQLException | ReflectiveOperationException | ParseException e){
            Logger.error("JdbcOperate.getObject error",e);
        }finally{
            // 非事务模式执行
            if (jdbcOperate.getTranscationType() == TranscationType.NONE) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return null;
    }

    public Map<String, Object> getMap(){
        try{
            if(resultSet.next()){
                return TSQL.getOneRowWithMap(this.resultSet);
            }else{
                return null;
            }
        }catch(SQLException | ReflectiveOperationException e){
            Logger.error("JdbcOperate.getMap error",e);
        }finally{
            // 非事务模式执行
            if (jdbcOperate.getTranscationType() == TranscationType.NONE) {
                JdbcOperate.closeConnection(resultSet);
            }else{
                JdbcOperate.closeResult(resultSet);
            }
        }
        return null;
    }
}

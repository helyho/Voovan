package org.voovan.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.voovan.tools.log.Logger;

/**
 * SQL处理帮助类
 * 
 * 注意所有的时间都用yyyy-MM-dd HH:mm:ss的格式
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TSQL {
	/**
	 * 从 SQL 字符串中,取 SQL 参数表
	 * @param sqlStr  原始 sql 字符串 (select * from table where x=:x and y=:y)
	 * @return  sql 参数对照表 ([:x,:y])
	 */
	public static List<String> getSqlParams(String sqlStr){
		String[] params = TString.searchByRegex(sqlStr, ":[^ ]+");
		return Arrays.asList(params);
	}
	
	/**
	 * 转换preparedStatement对象为可用的 sql 字符串(参数用?表示)
	 * @param sqlStr		原始 sql 字符串 (select * from table where x=:x and y=:y)
	 * @param sqlParams		sql参数表
	 * @return				将所有的:引导的参数转换成? (select * from table where x=? and y=?)
	 */
	public static String preparedSql(String sqlStr){
		return TString.replaceByRegex(sqlStr, ":[^ ]+", "?");
	}
	
	/**
	 * 给preparedStatement对象设置参数
	 * 	
	 * @param preparedStatement  preparedStatement对象
	 * @param sqlParams			sql 参数表
	 * @param params			参数键值 Map
	 * @throws SQLException
	 */
	public static void setPreparedParams(PreparedStatement preparedStatement,List<String> sqlParams,Map<String, Object> params) throws SQLException{
		for(int i=0;i<sqlParams.size();i++){
			String paramName = sqlParams.get(i);
			//去掉前面:号
			paramName = paramName.substring(1,paramName.length());
			preparedStatement.setObject(i+1, params.get(paramName));
			Logger.debug("Parameter: ["+sqlParams.get(i)+" = "+params.get(paramName)+"]");
		}
	}
	
	/**
	 * 创建PreparedStatement
	 * @param conn      数据库连接
	 * @param sqlStr    sql 自负穿
	 * @param params    Map 参数
	 * @return			PreparedStatement 对象
	 * @throws SQLException
	 */
	public static PreparedStatement createPreparedStatement(Connection conn,String sqlStr,Map<String, Object> params) throws SQLException{
		Logger.debug("Executed: " + sqlStr);
		//获取参数列表
		List<String> sqlParams = TSQL.getSqlParams(sqlStr);
		//获取preparedStatement可用的 SQL
		String preparedSql = TSQL.preparedSql(sqlStr);
		PreparedStatement preparedStatement = (PreparedStatement) conn.prepareStatement(preparedSql);
		if(params!=null){
			//preparedStatement参数填充
			TSQL.setPreparedParams(preparedStatement,sqlParams,params);
		}
		return preparedStatement;
	}
	
	/**
	 * 将数组转换成 Map 
	 * 			key 位置坐标 
	 *          value 数组值
	 * @param objs    	待转换的数组
	 * @return
	 */
	public static Map<String, Object> arrayToMap(Object[] objs){
		HashMap<String ,Object> arrayMap = new HashMap<String ,Object>();
		for(int i=0;i<objs.length;i++){
			arrayMap.put(Integer.toString(i+1), objs[i]);
		}
		return arrayMap;
	}
	
	/**
	 * 使用数组参数的属性组装SQL
	 * @param sqlStr
	 * @param args
	 * @return
	 */
	public static String assembleSQLWithArray(String sqlStr,Object[] args){
		Map<String,Object> argMap = arrayToMap(args);
		return assembleSQLWithMap(sqlStr,argMap);
	}
	
	/**
	 * 使用argObjectj参数的属性组装SQL
	 * @param sqlStr
	 * @param argObjectj
	 * @return
	 * @throws Exception
	 */
	public static String assembleSQLWithObject(String sqlStr,Object argObjectj) throws Exception{
		
		//获取对象 (属性-值)Map
		Map<String,Object> argMap = TReflect.getMapfromObject(argObjectj);
		return assembleSQLWithMap(sqlStr,argMap);
	}
	
	/**
	 *  使用argMap参数的KV组装SQL
	 *  		SQL字符串中以:开始的相同字符串将被替换
	 * @param sqlStr
	 * @param argMap
	 * @return
	 */
	public static String assembleSQLWithMap(String sqlStr,Map<String ,Object> argMap) {		
		
		for(String key : argMap.keySet())
		{
			sqlStr = sqlStr.replaceAll(":"+key,getSQLString(argMap.get(key)));
		}
		return sqlStr;
	}
	
	
	/**
	 * 包装resultSet中单行记录成Map
	 * @param resultset
	 * @return
	 * @throws Exception
	 */
    public static Map<String, Object> getOneRowWithMap(ResultSet resultset) throws Exception{
		 
		HashMap<String, Object> resultMap = new HashMap<String,Object>();
		HashMap<String,Integer> columns = new HashMap<String,Integer>();
		
		//遍历结果集字段信息
		int columnCount = resultset.getMetaData().getColumnCount();
		for(int i=1;i<=columnCount;i++){
			columns.put(resultset.getMetaData().getColumnName(i),resultset.getMetaData().getColumnType(i));
		}
		
		//组装Map
		for(Entry<String, Integer> columnEntry : columns.entrySet())
		{
			String methodName =getDataMethod(columnEntry.getValue());
			Object value = TReflect.invokeMethod(resultset, methodName, columnEntry.getKey());
			resultMap.put(columnEntry.getKey(), value);
		}
		return resultMap;
	}
    
    /**
     * 包装resultSet中单行记录成指定对象
     * @param clazz
     * @param resultset
     * @return
     * @throws Exception
     */
    public static Object getOneRowWithObject(Class<?> clazz,ResultSet resultset) throws Exception{
    	Map<String,Object>rowMap = getOneRowWithMap(resultset);
    	return TReflect.getObjectFromMap(clazz, rowMap);
    }
    
    /**
     * 包装resultSet中所有记录成List,单行元素为Map
     * @param resultset
     * @return
     * @throws Exception
     */
    public static List<Map<String,Object>> getAllRowWithMapList(ResultSet resultset) throws Exception{
    	List<Map<String,Object>> resultList = new Vector<Map<String,Object>>();
    	while(resultset.next()){
    		resultList.add(getOneRowWithMap(resultset));
    	}
    	return resultList;
    }
    
    /**
     * 包装resultSet中所有记录成List,单行元素为指定对象
     * @param clazz
     * @param resultset
     * @return
     * @throws Exception
     */
    public static List<Object> getAllRowWithObjectList(Class<?> clazz,ResultSet resultset) throws Exception{
    	List<Object> resultList = new Vector<Object>();
    	while(resultset.next()){
    		resultList.add(getOneRowWithObject(clazz,resultset));
    	}
    	return resultList;
    }
	
	/**
	 * 将标准类型转换成可在SQL中进行封装的字符串
	 * 例如:String类型的对象转换成 'chs'
	 * @param argObj
	 * @return
	 */
	public static String getSQLString(Object argObj)
	{
		//处理List变成SQL语法的in操作字符串，包括两端的括号“（）”
		if(argObj instanceof List)
		{
			Object[] objects =((List<?>)argObj).toArray();
			String listValueStr="(";
			for(Object obj : objects)
			{
				String sqlValue = getSQLString(obj);
				if(sqlValue!=null)
					listValueStr+=sqlValue+",";
			}
			return TString.removeSuffix(listValueStr)+")";
		}
		//处理String
		else if(argObj instanceof String){
			return "\'"+argObj.toString()+"\'";
		}
		//处理Boolean
		else if(argObj instanceof Boolean){
			if((Boolean)argObj)
				return  "true";
			else
				return  "false";
		}
		//处理Date
		else if(argObj instanceof Date){
			String pattern = "yyyy-MM-dd HH:mm:ss";
			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
			return "'"+dateFormat.format(argObj)+"'";
		}
		//处理其他类型，全部转换成String
		else
		{
			return argObj.toString();
		}
	}
	
	/**
	 * 根据 SQL 类型判断 Result 改使用什么方法取值
	 * @param databaseType
	 * @return  方法名
	 */
	public static String getDataMethod(int databaseType){
		switch(databaseType){
			case java.sql.Types.CHAR : 
		         return  "getString";
			case java.sql.Types.VARCHAR : 
			         return "getString";
			case java.sql.Types.LONGVARCHAR : 
			         return "getString";
			case java.sql.Types.NCHAR : 
			         return "getString";
			case java.sql.Types.LONGNVARCHAR : 
			         return "getString";
			case java.sql.Types.NUMERIC : 
			         return  "getBigDecimal";
			case java.sql.Types.DECIMAL : 
			         return  "getBigDecimal";
			case java.sql.Types.BIT : 
			         return "getBoolean";
			case java.sql.Types.BOOLEAN : 
			         return  "getBoolean";
			case java.sql.Types.TINYINT : 
			         return  "getByte";
			case java.sql.Types.SMALLINT : 
			         return  "getShort";
			case java.sql.Types.INTEGER : 
			         return  "getInt";
			case java.sql.Types.BIGINT : 
			         return  "getLong";
			case java.sql.Types.REAL : 
			         return  "getFloat";
			case java.sql.Types.FLOAT : 
			         return  "getDouble";
			case java.sql.Types.DOUBLE : 
			         return  "getDouble";
			case java.sql.Types.BINARY : 
			         return  "getBytes";
			case java.sql.Types.VARBINARY : 
			         return  "getBytes";
			case java.sql.Types.LONGVARBINARY : 
			         return  "getBytes";
			case java.sql.Types.DATE : 
			         return  "getDate";
			case java.sql.Types.TIME : 
			         return  "getTime";
			case java.sql.Types.TIMESTAMP : 
			         return  "getTimestamp";
			case java.sql.Types.CLOB : 
			         return  "getClob";
			case java.sql.Types.BLOB : 
			         return  "getBlob";
			case java.sql.Types.ARRAY : 
			         return  "getArray";
			default:
					return "getString";
		}
	}
}

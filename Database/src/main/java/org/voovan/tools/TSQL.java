package org.voovan.tools;

import org.voovan.db.CallType;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;

/**
 * SQL处理帮助类
 *
 * 注意所有的时间都用TDateTime.STANDER_DATETIME_TEMPLATE的格式
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TSQL {
	/**
	 * 从 SQL 字符串中,取 SQL 参数表
	 * @param sqlStr  原始 sql 字符串 (select * from table where x=::x and y=::y)
	 * @return  sql 参数对照表 ([:x,:y])
	 */
	public static List<String> getSqlParamNames(String sqlStr){
		String[] params = TString.searchByRegex(sqlStr, "::[^,\\s\\)]+");
		ArrayList<String> sqlParamNames = new ArrayList<String>();
		for(String param : params){
			sqlParamNames.add(param);
		}
		return sqlParamNames;
	}

	/**
	 * 转换preparedStatement对象为可用的 sql 字符串(参数用?表示)
	 * @param sqlStr		原始 sql 字符串 (select * from table where x=:x and y=::y)
	 * @return				将所有的:引导的参数转换成? (select * from table where x=? and y=?)
	 */
	public static String preparedSql(String sqlStr){
		return TString.fastReplaceAll(sqlStr, "::[^,\\s\\)]+", "?");
	}

	/**
	 * 给preparedStatement对象设置参数
	 *
	 * @param preparedStatement  preparedStatement对象
	 * @param sqlParamNames			sql 参数表
	 * @param params			参数键值 Map
	 * @throws SQLException SQL 异常
	 */
	public static void setPreparedParams(PreparedStatement preparedStatement,List<String> sqlParamNames,Map<String, ?> params) throws SQLException{
		for(int i=0;i<sqlParamNames.size();i++){
			String paramName = sqlParamNames.get(i);
			//去掉前面::号
			paramName = paramName.substring(2,paramName.length());
			Object data = params.get(paramName);
			if(data==null){
				throw new NullPointerException("SQL param: "+paramName+ " is null");
			}
			if(TReflect.isBasicType(data.getClass())) {
				preparedStatement.setObject(i + 1, params.get(paramName));
			}else{
				//复杂对象类型,无法直接保存进数据库,进行 JSON 转换后保存
				preparedStatement.setObject(i + 1, JSON.toJSON(params.get(paramName)));
			}
		}
	}

	/**
	 * 创建PreparedStatement
	 * @param conn      数据库连接
	 * @param sqlStr    sql 自负穿
	 * @param params    Map 参数
	 * @return			PreparedStatement 对象
	 * @throws SQLException SQL 异常
	 */
	public static PreparedStatement createPreparedStatement(Connection conn,String sqlStr,Map<String, Object> params) throws SQLException{

		//获取参数列表
		List<String> sqlParamNames = TSQL.getSqlParamNames(sqlStr);

		//将没有提供查询参数的条件移除
		sqlStr = TSQL.removeEmptyCondiction(sqlStr,sqlParamNames,params);

		if(Logger.isLogLevel("DEBUG")) {
			Logger.debug("[SQL_Executed]: " + assembleSQLWithMap(sqlStr, params));
		}

		//获取preparedStatement可用的 SQL
		String preparedSql = TSQL.preparedSql(sqlStr);

		PreparedStatement preparedStatement = (PreparedStatement) conn.prepareStatement(preparedSql);

		//如果params为空,则新建一个
		if(params==null){
			params = new Hashtable<String, Object>();
		}

		//为preparedStatement参数填充
		TSQL.setPreparedParams(preparedStatement,sqlParamNames,params);
		return preparedStatement;
	}

	/**
	 * 创建PreparedStatement
	 * @param conn      数据库连接
	 * @param sqlStr    sql 自负穿
	 * @param params    Map 参数
	 * @param callTypes 调用参数类型
	 * @return			PreparedStatement 对象
	 * @throws SQLException SQL 异常
	 */
	public static CallableStatement createCallableStatement(Connection conn,String sqlStr,Map<String, Object> params, CallType[] callTypes) throws SQLException{
		Logger.debug("[SQL_Executed]: " + sqlStr);
		//获取参数列表
		List<String> sqlParamNames = TSQL.getSqlParamNames(sqlStr);
		//获取preparedStatement可用的 SQL
		String preparedSql = TSQL.preparedSql(sqlStr);

		//定义 jdbc statement 对象
		CallableStatement callableStatement = (CallableStatement) conn.prepareCall(preparedSql);

		//如果params为空,则新建一个
		if(params==null){
			params = new Hashtable<String, Object>();
		}

		//callableStatement参数填充
		TSQL.setPreparedParams(callableStatement,sqlParamNames,params);

		//根据存储过程参数定义,注册 OUT 参数
		ParameterMetaData parameterMetaData = callableStatement.getParameterMetaData();
		for(int i=0;i<parameterMetaData.getParameterCount();i++){
			int paramMode = parameterMetaData.getParameterMode(i+1);
			if(paramMode == ParameterMetaData.parameterModeOut || paramMode == ParameterMetaData.parameterModeInOut) {
				callableStatement.registerOutParameter(i + 1, parameterMetaData.getParameterType(i + 1));
			}
		}

		return callableStatement;
	}

	/**
	 * 解析存储过程结果集
	 * @param callableStatement  callableStatement对象
	 * @return 解析后的存储过程结果集
	 * @throws SQLException SQL 异常
	 */
	public static List<Object> getCallableStatementResult(CallableStatement callableStatement) throws SQLException{
		ArrayList<Object> result = new ArrayList<Object>();
		ParameterMetaData parameterMetaData =  callableStatement.getParameterMetaData();

		//遍历参数信息
		for(int i=0;i<parameterMetaData.getParameterCount();i++){
			int paramMode = parameterMetaData.getParameterMode(i+1);

			//如果是带有 out 属性的参数,则对其进行取值操作
			if(paramMode == ParameterMetaData.parameterModeOut || paramMode == ParameterMetaData.parameterModeInOut){
				//取值方法名
				String methodName = getDataMethod(parameterMetaData.getParameterType(i+1));
				Object value;
				try {
					//获得取值方法参数参数是 int 类型的对应方法
					Method method = TReflect.findMethod(CallableStatement.class,methodName,new Class[]{int.class});

					//反射调用方法
					value = TReflect.invokeMethod(callableStatement, method,i+1);
					result.add(value);

				} catch (ReflectiveOperationException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}



	/**
	 * 使用数组参数的属性组装SQL
	 * @param sqlStr SQL 字符串
	 * @param args 拼装参数
	 * @return 拼装后的 SQL
	 */
	public static String assembleSQLWithArray(String sqlStr,Object[] args){
		//获取对象  [序号, 值] Map
		Map<String,Object> argMap = TObject.arrayToMap(args);
		return assembleSQLWithMap(sqlStr,argMap);
	}

	/**
	 * 使用argObjectj参数的属性组装SQL
	 * @param sqlStr    SQL 字符串
	 * @param argObjectj 拼装对象
	 * @return 拼装候的SQL
	 * @throws ReflectiveOperationException  反射异常
	 */
	public static String assembleSQLWithObject(String sqlStr,Object argObjectj) throws ReflectiveOperationException{
		//获取对象 [属性-值] Map
		Map<String,Object> argMap = TReflect.getMapfromObject(argObjectj);
		return assembleSQLWithMap(sqlStr,argMap);
	}

	/**
	 *  使用argMap参数的KV组装SQL
	 *  		SQL字符串中以:开始的相同字符串将被替换
	 * @param sqlStr SQL 字符串
	 * @param argMap 拼装的 Map
	 * @return 拼装后的字符串
	 */
	public static String assembleSQLWithMap(String sqlStr,Map<String ,Object> argMap) {
		if(argMap!=null) {
			for (Entry<String, Object> arg : argMap.entrySet()) {
				sqlStr = TString.fastReplaceAll(sqlStr+" ", "::" + arg.getKey()+"\\s", getSQLString(argMap.get(arg.getKey()))+" ");
			}
		}
		return sqlStr.trim();
	}

	/**
	 * 包装resultSet中单行记录成Map
	 * @param resultset 查询结果集
	 * @return 转后的 Map 对象
	 * @throws SQLException  SQL 异常
	 * @throws ReflectiveOperationException  反射异常
	 */
	public static Map<String, Object> getOneRowWithMap(ResultSet resultset)
			throws SQLException, ReflectiveOperationException {

		HashMap<String, Object> resultMap = new HashMap<String,Object>();
		HashMap<String,Integer> columns = new HashMap<String,Integer>();

		//遍历结果集字段信息
		int columnCount = resultset.getMetaData().getColumnCount();
		for(int i=1;i<=columnCount;i++){
			columns.put(resultset.getMetaData().getColumnLabel(i),resultset.getMetaData().getColumnType(i));
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
	 * @param clazz 类对象
	 * @param resultset 查询结果集
	 * @return 转换后的对象
	 * @throws ReflectiveOperationException 反射异常
	 * @throws SQLException  SQL 异常
	 * @throws ParseException  解析异常
	 */
	public static Object getOneRowWithObject(Class<?> clazz,ResultSet resultset)
			throws SQLException, ReflectiveOperationException, ParseException {
		Map<String,Object>rowMap = getOneRowWithMap(resultset);

		//对象转换时,模糊匹配属性,去除掉所有的
		HashMap<String,Object> newMap = new HashMap<String,Object>();
		for(Entry<String,Object> entry : rowMap.entrySet()){
			String key = TString.fastReplaceAll(entry.getKey(), "[^a-z|A-Z|0-9]", "");
			newMap.put(key,entry.getValue());
		}
		rowMap.clear();

		return TReflect.getObjectFromMap(clazz, newMap,true);
	}

	/**
	 * 包装resultSet中所有记录成List,单行元素为Map
	 * @param resultSet 查询结果集
	 * @return 转后的 List[Map]
	 * @throws ReflectiveOperationException 反射异常
	 * @throws SQLException  SQL 异常
	 */
	public static List<Map<String,Object>> getAllRowWithMapList(ResultSet resultSet)
			throws SQLException, ReflectiveOperationException {
		List<Map<String,Object>> resultList = new ArrayList<Map<String,Object>>();
		while(resultSet!=null && resultSet.next()){
			resultList.add(getOneRowWithMap(resultSet));
		}
		return resultList;
	}

	/**
	 * 包装resultSet中所有记录成List,单行元素为指定对象
	 * @param clazz 类
	 * @param resultSet 查询结果集
	 * @return 转换候的对象结合
	 * @throws ParseException  解析异常
	 * @throws ReflectiveOperationException 反射异常
	 * @throws SQLException  SQL 异常
	 */
	public static List<Object> getAllRowWithObjectList(Class<?> clazz,ResultSet resultSet)
			throws SQLException, ReflectiveOperationException, ParseException {
		List<Object> resultList = new ArrayList<Object>();
		while(resultSet!=null && resultSet.next()){
			resultList.add(getOneRowWithObject(clazz,resultSet));
		}
		return resultList;
	}


	/**
	 * 将SQL 语句中,没有提供查询参数的条件移除
	 * @param sqlText SQL 字符串
	 * @param sqlParamNames sql 参数名集合
	 * @param params 参数集合
	 * @return 转换后的字符串
	 */
	public static String removeEmptyCondiction(String sqlText,List<String> sqlParamNames,Map<String, Object> params){

		//如果params为空,则新建一个
		if(params==null){
			params = new Hashtable<String, Object>();
		}

		//转换存在参数的变量从::paramName 到 ``paramName
		for(String paramName : params.keySet()){
			sqlText = sqlText.replace("::"+paramName,"``"+paramName);
		}

		String sqlRegx = "((\\swhere\\s)|(\\sand\\s)|(\\sor\\s))[\\S\\s]+?(?=(\\swhere\\s)|(\\s\\)\\s)|(\\sand\\s)|(\\sor\\s)|(\\sorder\\s)|(\\shaving\\s)|$)";
		String[] sqlCondiction = TString.searchByRegex(sqlText,sqlRegx);
		for(String condiction : sqlCondiction){
			String[] condictions = TString.searchByRegex(condiction,"::[^,\\s\\)]+");
			if(condictions.length>0){
				if(condiction.trim().toLowerCase().startsWith("where")){
					sqlText = sqlText.replace(condiction.trim(),"where 1=1");
				}else{
					sqlText = sqlText.replace(condiction.trim(),"");
				}
				sqlParamNames.remove(condictions[0]);
			}
		}

		//转换存在参数的变量从``paramName 到 ::paramName
		return sqlText.replace("``","::");
	}

	/**
	 * 获取解析后的 SQL 的条件
	 * @param sqlText SQL 字符串
	 * @return 解析的 SQL 查询条件
	 */
	public static List<String[]> parseSQLCondiction(String sqlText) {
		ArrayList<String[]> condictionList = new ArrayList<String[]>();
		sqlText = sqlText.toLowerCase();
		String sqlRegx = "((\\swhere\\s)|(\\sand\\s)|(\\sor\\s))[\\S\\s]+?(?=(\\swhere\\s)|(\\s\\)\\s)|(\\sand\\s)|(\\sor\\s)|(\\sorder\\s)|(\\shaving\\s)|$)";
		String[] sqlCondiction = TString.searchByRegex(sqlText,sqlRegx);
		for(String condiction : sqlCondiction){
			condiction = condiction.trim();
			String concateMethod = condiction.substring(0,condiction.indexOf(" ")+1).trim();
			condiction = condiction.substring(condiction.indexOf(" ")+1,condiction.length()).trim();
			String operatorChar = TString.searchByRegex(condiction, "(\\slike\\s*)|(\\sin\\s*)|(>=)|(<=)|[=<>]")[0].trim();


			String[] condictionArr = condiction.split("(\\slike\\s*)|(\\sin\\s*)|(>=)|(<=)|[=<>]");
			condictionArr[0] = condictionArr[0].trim();
			condictionArr[1] = condictionArr[1].trim();
			if(condictionArr[0].trim().indexOf(".")>1){

				condictionArr[0] = condictionArr[0].split("\\.")[1];
				condictionArr[0] = condictionArr[0].substring(condictionArr[0].lastIndexOf(" ")+1);
			}

			if(condictionArr.length>1){
				if((condictionArr[1].trim().startsWith("'") && condictionArr[1].trim().endsWith("'")) ||
						(condictionArr[1].trim().startsWith("(") && condictionArr[1].trim().endsWith(")"))
						){
					condictionArr[1] = condictionArr[1].substring(1,condictionArr[1].length()-1);
				}
				if(operatorChar.contains("in")){
					condictionArr[1] = condictionArr[1].replace("'", "");
				}
				//System.out.println("操作符: "+concateMethod+" \t查询字段: "+condictionArr[0]+" \t查询关系: "+operatorChar+" \t查询值: "+condictionArr[1]);
				condictionList.add(new String[]{concateMethod, condictionArr[0], operatorChar, condictionArr[1]});
			}else{
				Logger.error("Parse SQL condiction error");
			}

		}
		return condictionList;
	}


	/**
	 * SQL的参数,将 JAVA 的类型转换成可在SQL中进行封装的字符串
	 * 例如:String类型的对象转换成 'chs'
	 * @param argObj 转换前的对象
	 * @return 封装后的字符串
	 */
	public static String getSQLString(Object argObj)
	{
		if(argObj==null){
			return "null";
		}
		//处理List变成SQL语法的in操作字符串，包括两端的括号“（）”
		if(argObj instanceof List)
		{
			Object[] objects =((List<?>)argObj).toArray();
			StringBuilder listValueStr= new StringBuilder("(");
			for(Object obj : objects)
			{
				String sqlValue = getSQLString(obj);
				if(sqlValue!=null) {
					listValueStr.append(sqlValue);
					listValueStr.append(",");
				}
			}
			return TString.removeSuffix(listValueStr.toString())+")";
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
			SimpleDateFormat dateFormat = new SimpleDateFormat(TDateTime.STANDER_DATETIME_TEMPLATE);
			return "'"+dateFormat.format(argObj)+"'";
		}
		//处理其他类型，全部转换成String
		else
		{
			return argObj.toString();
		}
	}

	/**
	 * 根据 SQL 类型判断 Result 该使用什么方法取值
	 * @param databaseType 数据库中的数据类型
	 * @return  方法名
	 */
	public static String getDataMethod(int databaseType){
		switch(databaseType){
			case Types.CHAR :
				return  "getString";
			case Types.VARCHAR :
				return "getString";
			case Types.LONGVARCHAR :
				return "getString";
			case Types.NCHAR :
				return "getString";
			case Types.LONGNVARCHAR :
				return "getString";
			case Types.NUMERIC :
				return  "getBigDecimal";
			case Types.DECIMAL :
				return  "getBigDecimal";
			case Types.BIT :
				return "getBoolean";
			case Types.BOOLEAN :
				return  "getBoolean";
			case Types.TINYINT :
				return  "getByte";
			case Types.SMALLINT :
				return  "getShort";
			case Types.INTEGER :
				return  "getInt";
			case Types.BIGINT :
				return  "getLong";
			case Types.REAL :
				return  "getFloat";
			case Types.FLOAT :
				return  "getFloat";
			case Types.DOUBLE :
				return  "getDouble";
			case Types.BINARY :
				return  "getBytes";
			case Types.VARBINARY :
				return  "getBytes";
			case Types.LONGVARBINARY :
				return  "getBytes";
			case Types.DATE :
				return  "getDate";
			case Types.TIME :
				return  "getTime";
			case Types.TIMESTAMP :
				return  "getTimestamp";
			case Types.CLOB :
				return  "getClob";
			case Types.BLOB :
				return  "getBlob";
			case Types.ARRAY :
				return  "getArray";
			default:
				return "getString";
		}
	}

	/**
	 * 根据 JAVA 类型判断该使用什么 SQL 数据类型
	 * @param obj 对象
	 * @return 数据库中的数据类型
	 */
	public static int getSqlTypes(Object obj){
		Class<?> objectClass = obj.getClass();
		if(char.class == objectClass){
			return  Types.CHAR;
		}else if(String.class == objectClass){
			return Types.VARCHAR ;
		}else if(BigDecimal.class == objectClass){
			return Types.NUMERIC;
		}else if(Boolean.class == objectClass){
			return Types.BIT;
		}else if(Byte.class == objectClass){
			return Types.TINYINT;
		}else if(Short.class == objectClass){
			return Types.SMALLINT;
		}else if(Integer.class == objectClass){
			return Types.INTEGER;
		}else if(Long.class == objectClass){
			return Types.BIGINT;
		}else if(Float.class == objectClass){
			return Types.FLOAT;
		}else if(Double.class == objectClass){
			return Types.DOUBLE;
		}else if(Byte[].class == objectClass){
			return Types.BINARY;
		}else if(Date.class == objectClass){
			return Types.DATE;
		}else if(Time.class == objectClass){
			return Types.TIME;
		}else if(Timestamp.class == objectClass){
			return Types.TIMESTAMP;
		}else if(Clob.class == objectClass){
			return Types.CLOB;
		}else if(Blob.class == objectClass){
			return Types.BLOB;
		}else if(Object[].class == objectClass){
			return Types.ARRAY;
		}
		return 0;
	}
}

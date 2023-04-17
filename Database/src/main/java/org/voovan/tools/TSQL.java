package org.voovan.tools;

import org.voovan.db.CallType;
import org.voovan.db.DataBaseType;
import org.voovan.db.recorder.Recorder;
import org.voovan.db.recorder.annotation.NotInsert;
import org.voovan.db.recorder.annotation.NotUpdate;
import org.voovan.db.recorder.annotation.PrimaryKey;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	public static final Map<String, Object> EMPTE_MAP = new Hashtable<String, Object>();

	/**
	 * 从 SQL 字符串中,取 SQL 参数表
	 * @param sqlStr  原始 sql 字符串 (select * from table where x=::x and y=::y)
	 * @return  sql 参数对照表 ([:x,:y])
	 */
	public static List<String> getSqlParamNames(String sqlStr){
		String[] params = TString.searchByRegex(sqlStr, "::\\w+\\b");
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
		return TString.fastReplaceAll(sqlStr, "::\\w+\\b", "?");
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

			if(data==null || TReflect.isBasicType(data.getClass())) {
				preparedStatement.setObject(i + 1, data);
			} else if(data instanceof BigDecimal){
				preparedStatement.setObject(i + 1, ((BigDecimal)data).toPlainString());
			} else if(data instanceof Array) {
				preparedStatement.setObject(i + 1, ((Array) data).getArray());
			} else if(data instanceof Date) {
				preparedStatement.setObject(i + 1, data);
			} else {
				//复杂对象类型,无法直接保存进数据库,进行 JSON 转换后保存
				preparedStatement.setObject(i + 1, JSON.toJSON(data));
			}
		}
	}
	//=============================================================================================

	/**
	 * 创建PreparedStatement
	 * @param conn      数据库连接
	 * @param sqlStr    sql 自负穿
	 * @param params    Map 参数
	 * @return			PreparedStatement 对象
	 * @throws SQLException SQL 异常
	 */
	public static PreparedStatement createPreparedStatement(Connection conn,String sqlStr, Map<String, Object> params) throws SQLException{

		//将没有提供查询参数的条件移除
		sqlStr = TSQL.removeEmptyCondiction(sqlStr, params);

		//获取参数列表
		List<String> sqlParamNames = TSQL.getSqlParamNames(sqlStr);

        Logger.sql("[SQL_Executed]: " + assembleSQLWithMap(sqlStr, params));

		//获取preparedStatement可用的 SQL
		String preparedSql = TSQL.preparedSql(sqlStr);

		PreparedStatement preparedStatement = (PreparedStatement) conn.prepareStatement(preparedSql);

		//如果params为空,则新建一个
		if(params==null){
			params = EMPTE_MAP;
		}

		//为preparedStatement参数填充
		TSQL.setPreparedParams(preparedStatement, sqlParamNames, params);
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
		Logger.sql("[SQL_Executed]: " + sqlStr);
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
					Logger.error(e);
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
		Map<String,Object> argMap = TReflect.getMapFromObject(argObjectj);
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
				sqlStr = TString.fastReplaceAll(sqlStr+" ", "::" + arg.getKey()+"\\b", getSQLString(argMap.get(arg.getKey())));
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

		LinkedHashMap<String, Object> resultMap = new LinkedHashMap<String,Object>();
		LinkedHashMap<String,Integer> columns = new LinkedHashMap<String,Integer>();

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
	public static Object getOneRowWithObject(Class<?> clazz, ResultSet resultset)
			throws SQLException, ReflectiveOperationException, ParseException {
		Map<String,Object>rowMap = getOneRowWithMap(resultset);
		return TReflect.getObjectFromMap(clazz, rowMap, true);
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


	private static String EQUAL_CONDICTION = " 1=1";
	private static String NOT_EQUAL_CONDICTION = " 1!=1";
	/**
	 * 将SQL 语句中,没有提供查询参数的条件移除
	 * @param sqlText SQL 字符串
	 * @param params 参数集合
	 * @return 转换后的字符串
	 */
	public static String removeEmptyCondiction(String sqlText, Map<String, Object> params){
		//insert 不处理查询参数的条件移除
		if(sqlText.startsWith("insert") || sqlText.startsWith("INSERT")) {
			return sqlText;
		}

		//如果params为空,则新建一个
		if(params==null){
			params = new Hashtable<String, Object>();
		}

		List<String[]> condictionList = parseSQLCondiction(sqlText);
		for(String[] condictionArr : condictionList){
			//检查条件是否带参数, 检查参数是否存在
			sqlText = processCondition( condictionArr,sqlText, params);
		}

		return sqlText;
	}

	public static String processCondition(String[] condictionArr, String sqlText, Map<String, Object> params){

		String orginCondiction 			= condictionArr[0];
		String beforeCondictionMethod 	= condictionArr[1];
		String condictionName 			= condictionArr[2];
		String operatorChar 			= condictionArr[3];
		String originCondictionParams   = condictionArr[4];
		String afterCondictionMethod 	= condictionArr[5];

		String replaceCondiction = orginCondiction;
		String condictionParams = originCondictionParams;

		if(originCondictionParams!=null && originCondictionParams.contains("::")) {
			//找出所有的参数
			String[] condictions = TString.searchByRegex(originCondictionParams, "::\\w+\\b");
			if(condictions.length > 0) {
				for (String condictionParam : condictions){

					//判断参数是否存在并做移除的处理
					if (!params.containsKey(condictionParam.replace("::", ""))) {

						//遍历所有的 in 的条件, 去除没有参数的条件 例如: [::status,1,2] -> [1,2]
						if(operatorChar.equals("in") || operatorChar.equals("not in")) {

							condictionParams = TString.fastReplaceAll(condictionParams, condictionParam+"\\s*,?", "");
						}
						//遍历常规条件
						else {
							replaceCondiction = EQUAL_CONDICTION;
							if ("or".equals(beforeCondictionMethod) || "or".equals(afterCondictionMethod)) {
								replaceCondiction = NOT_EQUAL_CONDICTION;
							}
							//从原查询条件, 生成替换用的查询条件, 这样可以保留原查询条件种的 ( 或 )
							originCondictionParams = genReplaceConditionParams(originCondictionParams);

							String targetCondiction = condictionName + "\\s*" + operatorChar + "\\s*" + originCondictionParams;

							replaceCondiction = TString.fastReplaceAll(orginCondiction, targetCondiction , replaceCondiction, true);

							break;
						}
					}
				}

				//in 或者 not in 之前已经有移除了参数, 这里做最后的, 处理
				if(operatorChar.equals("in") || operatorChar.equals("not in")) {
					condictionParams = condictionParams.trim();
					if(condictionParams.endsWith(",")){
						condictionParams = TString.removeSuffix(condictionParams);
					}

					originCondictionParams = genReplaceConditionParams(originCondictionParams);

					replaceCondiction = TString.fastReplaceAll(replaceCondiction, originCondictionParams, condictionParams, true);
				}

				sqlText = sqlText.replace(orginCondiction, replaceCondiction);
			}
		}

		return  sqlText;

	}

	/**
	 * 从原查询条件, 生成替换用的查询条件, 这样可以保留原查询条件种的 ( 或 )
	 * @param originCondictionParams 原查询条件
	 * @return 替换用的查询条件
	 */
	public static String genReplaceConditionParams(String originCondictionParams){

		originCondictionParams = originCondictionParams.replaceAll("\\(", "\\\\(");
		originCondictionParams = originCondictionParams.replaceAll("\\)", "\\\\)");
		originCondictionParams = originCondictionParams.replaceAll("\\[", "\\\\[");
		originCondictionParams = originCondictionParams.replaceAll("\\]", "\\\\]");
		originCondictionParams = originCondictionParams.replaceAll("\\+", "\\\\+");
		originCondictionParams = originCondictionParams.replaceAll("\\*", "\\\\*");

		return originCondictionParams;
	}

		/**
	 * 将SQL 语句中,没有提供查询参数的条件移除
	 * @param sqlText SQL 字符串
	 * @param params 参数集合
	 * @return 转换后的字符串
	 */
	public static String removeEmptyCondiction(String sqlText, Object ... params){
    	return TSQL.removeEmptyCondiction(sqlText, TObject.removeMapNullValue(TObject.arrayToMap(params), true));
	}


	public static ConcurrentHashMap<Integer, List<String[]>> PARSED_CONDICTIONS = new ConcurrentHashMap<Integer,  List<String[]>>();
	public static AtomicInteger size = new AtomicInteger();
	/**
	 * 获取解析后的 SQL 的条件
	 * @param sqlText SQL 字符串
	 * @return 解析的 SQL 查询条件
	 */
	public static List<String[]> parseSQLCondiction(String sqlText) {
		int hashcode = THash.HashFNV1(sqlText);
		List<String[]> condictionList = PARSED_CONDICTIONS.get(hashcode);

		if(condictionList == null) {
			if(size.get() > 10000) {
				synchronized (PARSED_CONDICTIONS) {
					synchronized(size) {
						Logger.warn("SQL may be has same problem on TSQL.parseSQLCondiction: " + sqlText);
						PARSED_CONDICTIONS.clear();
						size.set(0);
					}
				}
			}

			condictionList = new ArrayList<String[]>();
			String sqlRegx = "((\\swhere\\s)|(\\sand\\s)|(\\sor\\s))[\\S\\s]+?(?=(\\swhere\\s)|(\\sand\\s)|(\\sor\\s)|(\\sgroup by\\s)|(\\sorder\\s)|(\\slimit\\s)|$)";
			String[] sqlCondictions = TString.searchByRegex(sqlText, sqlRegx, Pattern.CASE_INSENSITIVE);
			for (int i = 0; i < sqlCondictions.length; i++) {
				String condiction = sqlCondictions[i];

				//如果包含 ) 并且不在字符串中, 则移除后面的内容, 防止解析出 1=1) m2, gggg m3  导致替换问题
//				if (condiction.indexOf("(") < condiction.lastIndexOf(")") && TString.searchByRegex(condiction, "[\\\"`'].*?\\).*?[\\\"`']").length == 0) {
//					int indexClosePair = condiction.lastIndexOf(")");
//					if (indexClosePair != -1) {
//						condiction = condiction.substring(0, indexClosePair + 1);
//					}
//				}

				//between 则拼接下一段
				if (TString.regexMatch(condiction, "\\sbetween\\s") > 0) {
					i = i + 1;
					condiction = condiction + sqlCondictions[i];
				}

				String originCondiction = condiction;
				condiction = condiction.trim();
				String concateMethod = condiction.substring(0, condiction.indexOf(" ") + 1).trim();
				condiction = condiction.substring(condiction.indexOf(" ") + 1, condiction.length()).trim();
				String[] splitedCondicction = TString.searchByRegex(condiction, "(\\sbetween\\s+)|(\\sis\\s+)|(\\slike\\s+)|(\\s(not\\s)?in\\s+)|(\\!=)|(>=)|(<=)|[=<>]");
				if (splitedCondicction.length == 1) {
					String operatorChar = splitedCondicction[0].trim();
					String[] condictionArr = condiction.split("(\\sbetween\\s+)|(\\sis\\s+)|(\\slike\\s+)|(\\s(not\\s)?in\\s+)|(\\!=)|(>=)|(<=)|[=<>]");
					condictionArr[0] = condictionArr[0].trim();

					//查询的主字段 ( 的处理
					if (TString.regexMatch(condiction, "\\(") > TString.regexMatch(condiction, "\\)") && condictionArr[0].startsWith("(")) {
						condictionArr[0] = TString.removePrefix(condictionArr[0]);
					}

					condictionArr[1] = condictionArr[1].trim();

					if (condictionArr.length > 1) {
						if (operatorChar.contains("in") && condictionArr[1].trim().startsWith("(") && condictionArr[1].trim().endsWith(")")) {
							condictionArr[1] = condictionArr[1].substring(1, condictionArr[1].length() - 1);
						}

						if (condictionArr[0].startsWith("(") && TString.regexMatch(condictionArr[1], "\\(") > TString.regexMatch(condictionArr[1], "\\)")) {
							condictionArr[0] = TString.removePrefix(condictionArr[0]);
						}

						if (condictionArr[1].endsWith(")") && TString.regexMatch(condictionArr[1], "\\(") < TString.regexMatch(condictionArr[1], "\\)")) {
							condictionArr[1] = TString.removeSuffix(condictionArr[1]);
						}

						condictionList.add(new String[]{originCondiction.trim(), concateMethod, condictionArr[0].trim(), operatorChar, condictionArr[1].trim(), null});
					} else {
						Logger.error("Parse SQL condiction error");
					}
				} else {
					condictionList.add(new String[]{originCondiction, null, null, null, null, null});
				}
			}

			for (int i = condictionList.size() - 2; i >= 0; i--) {
				condictionList.get(i)[5] = condictionList.get(i + 1)[1];
			}

			size.getAndIncrement();
			PARSED_CONDICTIONS.put(hashcode, condictionList);
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
	 * @param clazz 类对象
	 * @return 数据库中的数据类型
	 */
	public static String getSqlTypes(Class clazz){
		clazz = TReflect.getPackageClass(clazz);

		if(char.class == clazz){
			return  "char (5)";
		}else if(String.class == clazz){
			return "varchar (40)";
		}else if(BigDecimal.class == clazz){
			return "decimal (30,8)";
		}else if(Boolean.class == clazz){
			return "tinyint (1)";
		}else if(Byte.class == clazz){
			return "bit";
		}else if(Short.class == clazz){
			return "smallint(6)";
		}else if(Integer.class == clazz){
			return "integer(11)";
		}else if(Long.class == clazz){
			return "Bigint";
		}else if(Float.class == clazz){
			return "float";
		}else if(Double.class == clazz){
			return "double";
		}else if(Byte[].class == clazz){
			return "binary";
		}else if(Date.class == clazz){
			return "date";
		}else if(Time.class == clazz){
			return "time";
		}else if(Timestamp.class == clazz){
			return "timestamp(6)";
		}else if(Clob.class == clazz){
			return "clob";
		}else if(Blob.class == clazz){
			return "blob";
		}else if(Object[].class == clazz){
			return "array";
		}else {
			//Object, List, Map, ComplexObject
			return "varchar (2048)";
		}
	}

	/**
	 * 获取当前连接的数据库类型
	 *
	 * @param connection 连接对象
	 * @return 数据库类型
	 */
	private static Map<DataSource, DataBaseType> DATABASE_TYPE_MAP = new ConcurrentHashMap<DataSource, DataBaseType>();
	public static DataBaseType getDataBaseType(DataSource dataSource) {
		Connection connection = null;
		DataBaseType dataBaseType = DataBaseType.UNKNOW;

		try {
			dataBaseType = DATABASE_TYPE_MAP.get(dataSource);
			if (dataBaseType == null) {
				connection = dataSource.getConnection();
				String driverName = connection.getMetaData().getDriverName();

				//通过driverName是否包含关键字判断
				if (connection.getMetaData().getDriverName().toUpperCase().indexOf("MYSQL") != -1) {
					dataBaseType = DataBaseType.MySql;
				} else if (connection.getMetaData().getDriverName().toUpperCase().indexOf("MARIADB") != -1) {
					dataBaseType = DataBaseType.Mariadb;
				} else if (connection.getMetaData().getDriverName().toUpperCase().indexOf("POSTAGE") != -1) {
					dataBaseType = DataBaseType.Postgre;
				} else if (connection.getMetaData().getDriverName().toUpperCase().indexOf("ORACLE") != -1) {
					dataBaseType = DataBaseType.Oracle;
				} else {
					dataBaseType = DataBaseType.UNKNOW;
				}

				DATABASE_TYPE_MAP.put(dataSource, dataBaseType);
			}
		} catch (SQLException e) {
			dataBaseType = DataBaseType.UNKNOW;
		} finally {
			if(connection!=null) {
				try {
					connection.close();
				} catch (SQLException e) {
					Logger.error(e);
				}
			}
		}

		return dataBaseType;

	}


	/**
	 * 包括 SQL 关键字
	 * @param dataBaseType DataBaseType 枚举
	 * @param sqlField sql 关键字
	 * @return sql字符串
	 */
	public static String wrapSqlField(DataBaseType dataBaseType, String sqlField) {
		if (dataBaseType.equals(DataBaseType.Mariadb) || dataBaseType.equals(DataBaseType.MySql)) {
			return "`"+sqlField+"`";
		} else if (dataBaseType.equals(DataBaseType.Oracle)) {
			return "\""+sqlField+"\"";
		} else if (dataBaseType.equals(DataBaseType.Postgre)) {
			return "`"+sqlField+"`";
		} else {
			return sqlField;
		}
	}

	/**
	 * 生成 Mysql 分页的 sql
	 * @param sql Sql语句
	 * @param pageNumber 页码
	 * @param pageSize 页面记录数
	 * @return sql字符串
	 */
	public static String genMysqlPageSql(String sql, int pageNumber, int pageSize){
		int pageStart 	= (pageNumber-1) * pageSize;

		if(pageStart<0 || pageNumber<0) {
			return sql;
		}

		return sql + " limit " + pageStart + ", " + pageSize;
	}

	/**
	 * 生成 Postgre 分页的 sql
	 * @param sql Sql语句
	 * @param pageNumber 页码
	 * @param pageSize 页面记录数
	 * @return sql字符串
	 */
	public static String genPostgrePageSql(String sql, int pageNumber, int pageSize){
		int pageStart 	= (pageNumber-1) * pageSize;

		if(pageStart<0 || pageNumber<0) {
			return sql;
		}

		return sql + " limit " + pageStart + " offset " + pageStart;
	}

	/**
	 * 生成Oracle 分页的 sql
	 * @param sql Sql语句
	 * @param pageNumber 页码
	 * @param pageSize 页面记录数
	 * @return sql字符串
	 */
	public static String genOraclePageSql(String sql, int pageNumber, int pageSize){
		int pageStart 	= (pageNumber-1) * pageSize;
		int pageEnd 	= pageStart + pageSize;

		if(pageStart<0 || pageNumber<0) {
			return sql;
		}

		sql = sql.replaceFirst("select", "select rownum rn,");
		sql = "select pageSql.* from (" + sql + " ) pageSql where rn between " + pageStart + " and " + pageEnd;
		return sql;
	}

	public static String createTable(Class clazz, boolean underLine) {
		Object obj = null;
		try {
			obj = TReflect.newInstance(clazz, null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
		List<Field> fields = Arrays.stream(TReflect.getFields(clazz)).filter(f -> !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());

		String primaryKeyName = null;

		String createTableSQL = "CREATE TABLE `" + Recorder.getSqlTableName(obj) + "` ( \r\n";
		String columnDefine = "";
		for(Field field : fields) {
			if(field.isAnnotationPresent(NotInsert.class) || field.isAnnotationPresent(NotUpdate.class) ) {
				continue;
			}

			String fieldName = underLine ? TString.camelToUnderline(field.getName()) : field.getName();
			String oneColumn = "\t`" + fieldName + "` " + getSqlTypes(field.getType());

			if(field.isAnnotationPresent(PrimaryKey.class)) {
				primaryKeyName = field.getName();
				columnDefine = oneColumn + " not null, \r\n" + columnDefine;
			} else {
				columnDefine = columnDefine + oneColumn + " default null, \r\n";
			}
		}

		createTableSQL += columnDefine;

		if(primaryKeyName!=null) {
			createTableSQL += "\tprimary key (`" + primaryKeyName + "`)";
		} else {
			createTableSQL = TString.removeSuffix(createTableSQL.trim());
		}

		return createTableSQL + "\r\n);";


	}
}


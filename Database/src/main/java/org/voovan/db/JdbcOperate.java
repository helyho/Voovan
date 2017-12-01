package org.voovan.db;

import org.voovan.tools.TObject;
import org.voovan.tools.TSQL;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import javax.sql.DataSource;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * jdbc 操作类
 * 		每个数据库操作函数 开启关闭一次连接. 使用:开始来标识参数,例如: Map 和对象形式用:":arg", List 和 Array
 * 形式用":1"
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JdbcOperate {

	private DataSource	dataSource;
	private Connection	connection;
	private boolean		isTrancation;

	/**
	 * 构造函数
	 * @param dataSource	数据源
	 */
	public JdbcOperate(DataSource dataSource) {
		this.dataSource = dataSource;
		this.isTrancation = false;
	}

	/**
	 * 构造函数
	 * @param dataSource	数据源
	 * @param isTrancation   是否启用事物支持
	 */
	public JdbcOperate(DataSource dataSource,boolean isTrancation){
		this.dataSource = dataSource;
		this.isTrancation = isTrancation;
	}

	/**
	 * 获取连接
	 *
	 * @return 获取数据库连接
	 * @throws SQLException SQL 异常
	 */
	public Connection getConnection() throws SQLException {
		//如果连接不存在,或者连接已关闭则重取一个连接
		if (connection == null || connection.isClosed()) {
			connection = dataSource.getConnection();
			//如果是事务模式,则将自动提交设置为 false
			if (isTrancation) {
				connection.setAutoCommit(false);
			}
		}
		return connection;
	}

	/**
	 * 提交事物
	 * @param isClose 是否关闭数据库连接
	 * @throws SQLException SQL 异常
	 */
	public void commit(boolean isClose) throws SQLException{
		connection.commit();
		if(isClose) {
			closeConnection(connection);
		}
	}

	/**
	 * 回滚事物
	 * @param isClose 是否关闭数据库连接
	 * @throws SQLException SQL 异常
	 */
	public void rollback(boolean isClose) throws SQLException{
		connection.rollback();
		if(isClose) {
			closeConnection(connection);
		}
	}

	/**
	 * 提交事物不关闭连接
	 * @throws SQLException SQL 异常
	 */
	public void commit() throws SQLException{
		connection.commit();
	}

	/**
	 * 回滚事物不关闭连接
	 * @throws SQLException SQL 异常
	 */
	public void rollback() throws SQLException{
		connection.rollback();
	}

	/**
	 *
	 * @param sqlText
	 *            sql字符串
	 * @param mapArg
	 *            参数
	 * @return 结果集信息
	 * @throws SQLException SQL 异常
	 */
	private ResultInfo baseQuery(String sqlText, Map<String, Object> mapArg) throws SQLException {
		Connection conn = getConnection();
		try {
			//构造PreparedStatement
			PreparedStatement preparedStatement = TSQL.createPreparedStatement(conn, sqlText, mapArg);
			//执行查询
			ResultSet rs = preparedStatement.executeQuery();
			return new ResultInfo(rs,this.isTrancation);
		} catch (SQLException e) {
			closeConnection(conn);
			Logger.error("Query excution SQL Error! \n SQL is : \n\t" + sqlText + ": \n\t " ,e);
		}
		return null;
	}

	/**
	 * 执行数据库更新
	 *
	 * @param sqlText
	 *            sql字符串
	 * @param mapArg
	 *            参数
	 * @return 更新记录数
	 * @throws SQLException SQL 异常
	 */
	private int baseUpdate(String sqlText, Map<String, Object> mapArg) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement preparedStatement = null;
		SQLException exception = null;
		try {
			preparedStatement = TSQL.createPreparedStatement(conn, sqlText, mapArg);
			return preparedStatement.executeUpdate();
		} catch (SQLException e) {
			Logger.error("Update excution SQL Error! \n SQL is :\n\t " + sqlText + "\nError is: \n\t" ,e);
			exception = e;
		} finally {
			// 非事物模式执行
			if (!isTrancation) {
				closeConnection(preparedStatement);
			}else{
				if(exception!=null) {
					rollback();
				}
				closeStatement(preparedStatement);
			}
		}

		if(exception!=null){
			throw exception;
		}

		return -1;
	}

	/**
	 * 执行数据库批量更新
	 *
	 * @param sqlText
	 *            sql字符串
	 * @param mapArgs
	 *            参数
	 * @return 每条 SQL 更新记录数
	 * @throws SQLException SQL 异常
	 */
	private int[] baseBatch(String sqlText, List<Map<String, ?>> mapArgs) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement preparedStatement = null;
		SQLException exception = null;
		try {

			// 非事物模式执行
			if (!isTrancation) {
				conn.setAutoCommit(false);
			}

			// 获取 SQL 中的参数列表
			List<String> sqlParams = TSQL.getSqlParamNames(sqlText);
			preparedStatement = (PreparedStatement) conn.prepareStatement(TSQL.preparedSql(sqlText));
			if (mapArgs != null) {
				for (Map<String, ?> magArg : mapArgs) {
					// 用 sqlParams 对照 给 preparestatement 填充参数
					TSQL.setPreparedParams(preparedStatement, sqlParams, magArg);
					preparedStatement.addBatch();
				}
			}

			if(Logger.isLogLevel("DEBUG")) {
				Logger.debug("[SQL_Executed]: " + sqlText);
			}

			int[] result = preparedStatement.executeBatch();

			return result;
		} catch (SQLException e) {
			Logger.error("Batch excution SQL Error! \n SQL is : \n\t" + sqlText + ":\n\t" ,e);
			exception = e;
		} finally {
			// 非事物模式执行
			if (!isTrancation) {
				closeConnection(preparedStatement);
			}else{
				if(exception!=null) {
					rollback();
				}
				closeStatement(preparedStatement);
			}
		}

		if(exception!=null){
			throw exception;
		}

		return new int[0];
	}


	private List<Object> baseCall(String sqlText, CallType[] callTypes,Map<String, Object> mapArg) throws SQLException {
		Connection conn = getConnection();
		CallableStatement callableStatement = null;
		SQLException exception = null;
		try {
			callableStatement = TSQL.createCallableStatement(conn, sqlText, mapArg, callTypes);
			callableStatement.executeUpdate();
			List<Object> objList = TSQL.getCallableStatementResult(callableStatement);
			return objList;
		} catch (SQLException e) {
			Logger.error("Query excution SQL Error! \n SQL is : \n\t" + sqlText + ": \n\t " ,e);
			exception = e;
		} finally {
			// 非事物模式执行
			if (!isTrancation) {
				closeConnection(callableStatement);
			}else{
				if(exception!=null) {
					rollback();
				}
				closeStatement(callableStatement);
			}
		}

		if(exception!=null){
			throw exception;
		}

		return null;
	}


	/**
	 * 执行数据库更新
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @return 更新记录数
	 * @throws SQLException SQL 异常
	 */
	public int update(String sqlText) throws SQLException {
		return this.baseUpdate(sqlText, null);
	}

	/**
	 * 执行数据库更新,Object作为参数 字段名和对象属性名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param arg
	 *            object为参数的对象
	 * @return SQL 异常
	 * @throws ReflectiveOperationException 反射异常
	 * @throws SQLException SQL 异常
	 */
	public int update(String sqlText, Object arg) throws SQLException, ReflectiveOperationException {
		if(TReflect.isBasicType(arg.getClass())){
			return update(sqlText, arg, null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			return this.baseUpdate(sqlText, paramsMap);
		}
	}

	/**
	 * 执行数据库更新,Map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param mapArg
	 *            map为参数的对象
	 * @return 更新记录数
	 * @throws SQLException SQL 异常
	 */
	public int update(String sqlText, Map<String, Object> mapArg) throws SQLException {
		return this.baseUpdate(sqlText, mapArg);
	}

	/**
	 * 执行数据库更新,Map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,引导一个索引标识,索引标识从1开始,例如where id=::1
	 * @param args
	 *            多参数
	 * @return 更新记录数
	 * @throws SQLException  SQL 异常
	 */
	public int update(String sqlText, Object... args) throws SQLException {
		Map<String, Object> paramsMap = TObject.arrayToMap(args);
		return this.baseUpdate(sqlText, paramsMap);
	}

	/**
	 * 查询对象集合,无参数 字段名和对象属性名大消息必须大小写一致
	 *
	 * @param <T> 范型
	 *
	 * @param sqlText
	 *            sql字符串
	 * @param t
	 *            对象模型
	 * @return 结果集List
	 * @throws SQLException SQL 异常
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t) throws SQLException{
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo!=null){
			return (List<T>) resultInfo.getObjectList(t);
		}
		return new ArrayList<T>();

	}

	/**
	 * 查询单个对象,Object作为参数 字段名和对象属性名大消息必须大小写一致
	 *
	 * @param <T> 范型
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param arg
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return 结果集 List
	 * @throws ReflectiveOperationException  反射异常
	 * @throws SQLException  SQL 异常
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t, Object arg) throws SQLException, ReflectiveOperationException{
		if(TReflect.isBasicType(arg.getClass())){
			return queryObjectList(sqlText, t, arg,null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
			if(resultInfo!=null){
				return (List<T>) resultInfo.getObjectList(t);
			}
			return new ArrayList<T>();
		}
	}

	/**
	 * 查询对象集合,map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param <T>  范型
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param mapArg
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return  返回结果集 List
	 * @throws SQLException  SQL 异常
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t, Map<String, Object> mapArg) throws SQLException {
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo!=null){
			return (List<T>) resultInfo.getObjectList(t);
		}
		return new ArrayList<T>();
	}

	/**
	 * 查询对象集合,map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param <T> 范型
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,引导一个索引标识,索引标识从1开始,例如where id=::1
	 * @param args
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return 返回结果集 List
	 * @throws SQLException SQL 异常
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t, Object... args) throws SQLException{
		Map<String, Object> paramsMap = TObject.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo!=null){
			return (List<T>) resultInfo.getObjectList(t);
		}
		return new ArrayList<T>();
	}

	/**
	 * 查询对象集合,无参数
	 *
	 * @param sqlText
	 *            sql字符串
	 * @return 返回结果集 List[Map]
	 * @throws SQLException   SQL 异常
	 */
	public List<Map<String, Object>> queryMapList(String sqlText) throws SQLException {
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo!=null){
			return resultInfo.getMapList();
		}
		return new ArrayList<Map<String, Object>>();
	}

	/**
	 * 查询单个对象,Object作为参数
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param arg
	 *            Object参数
	 * @return 返回结果集 List[Map]
	 * @throws ReflectiveOperationException  反射异常
	 * @throws SQLException SQL 异常
	 */
	public List<Map<String, Object>> queryMapList(String sqlText, Object arg) throws SQLException, ReflectiveOperationException  {
		if(TReflect.isBasicType(arg.getClass())){
			return queryMapList(sqlText, arg, null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
			if(resultInfo!=null){
				return resultInfo.getMapList();
			}
			return new ArrayList<Map<String, Object>>();
		}
	}

	/**
	 * 查询对象集合,map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param mapArg
	 *            map参数
	 * @return 返回结果集 List[Map]
	 * @throws SQLException SQL 异常
	 */
	public List<Map<String, Object>> queryMapList(String sqlText, Map<String, Object> mapArg) throws SQLException{
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo!=null){
			return resultInfo.getMapList();
		}
		return new ArrayList<Map<String, Object>>();
	}

	/**
	 * 查询单个对象,Object作为参数
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,引导一个索引标识,索引标识从1开始,例如where id=::1
	 * @param args
	 *            Object参数
	 * @return 返回结果集 List[Map]
	 * @throws SQLException SQL 异常
	 */
	public List<Map<String, Object>> queryMapList(String sqlText, Object... args) throws SQLException {
		Map<String, Object> paramsMap = TObject.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo!=null){
			return resultInfo.getMapList();
		}
		return new ArrayList<Map<String, Object>>();
	}

	/**
	 * 查询单个对象,无参数
	 *
	 * @param <T> 范型
	 *
	 * @param sqlText
	 *            sql字符串
	 * @param t
	 *            对象模型
	 * @return 返回结果集 List[Map]
	 * @throws SQLException SQL 异常
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t) throws SQLException {
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo!=null){
			return (T) resultInfo.getObject(t);
		}
		return null;
	}

	/**
	 * 查询单个对象,Object作为参数 字段名和对象属性名大消息必须大小写一致
	 *
	 * @param <T> 范型
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param t
	 * 			  对象模型
	 * @param arg
	 *            Object参数
	 * @return 结果对象
	 * @throws ReflectiveOperationException 反射异常
	 * @throws SQLException SQL 异常
	 * @throws ParseException 解析异常
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Object arg) throws SQLException, ReflectiveOperationException, ParseException {
		if(TReflect.isBasicType(arg.getClass())){
			return queryObject(sqlText,t,arg,null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
			if(resultInfo!=null){
				return (T) resultInfo.getObject(t);
			}
			return null;
		}
	}

	/**
	 * 查询单个对象,map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param <T> 范型
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param mapArg
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return 结果对象
	 * @throws SQLException  SQL 异常
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Map<String, Object> mapArg) throws SQLException {
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo!=null){
			return (T) resultInfo.getObject(t);
		}
		return null;
	}

	/**
	 * 查询单个对象,map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param <T> 范型
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,引导一个索引标识,索引标识从1开始,例如where id=::1
	 * @param args
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return 结果对象
	 * @throws SQLException SQL 异常
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Object... args) throws SQLException{
		Map<String, Object> paramsMap = TObject.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo!=null){
			return (T) resultInfo.getObject(t);
		}
		return null;
	}

	/**
	 * 查询单行,返回 Map,无参数
	 *
	 * @param sqlText
	 *            sql字符串
	 * @return 结果对象 Map
	 * @throws SQLException SQL 异常
	 */
	public Map<String, Object> queryMap(String sqlText) throws SQLException {
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo!=null){
			return resultInfo.getMap();
		}
		return null;
	}



	/**
	 * 查询单行,返回 Map,Object作为参数
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"引导一个标识,例如where id=::id,中 id 就是标识。
	 * @param arg
	 *            arg参数 属性指代SQL字符串的标识,属性值用于在SQL字符串中替换标识。
	 * @return 结果对象 Map
	 * @throws SQLException SQL 异常
	 * @throws ReflectiveOperationException 反射异常
	 *
	 */
	public Map<String, Object> queryMap(String sqlText, Object arg) throws SQLException, ReflectiveOperationException {
		if(TReflect.isBasicType(arg.getClass())){
			return queryMap(sqlText,arg,null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
			if(resultInfo!=null){
				return resultInfo.getMap();
			}
			return null;
		}
	}

	/**
	 * 查询单行,返回 Map,map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"引导一个标识,例如where id=::id,中 id 就是标识。
	 * @param mapArg
	 *            map参数，key指代SQL字符串的标识,value用于在SQL字符串中替换标识。
	 * @return 结果对象 Map
	 * @throws SQLException SQL 异常
	 */
	public Map<String, Object> queryMap(String sqlText, Map<String, Object> mapArg) throws SQLException {
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo!=null){
			return resultInfo.getMap();
		}
		return null;
	}

	/**
	 * 查询单行,返回 Map,Array作为参数
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"引导一个索引标识,引导一个索引标识,索引标识从1开始,例如where id=::1
	 * @param args
	 *            args参数
	 * @return 结果对象
	 * @throws SQLException SQL 异常
	 */
	public Map<String, Object> queryMap(String sqlText, Object... args) throws SQLException {
		Map<String, Object> paramsMap = TObject.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo!=null){
			return resultInfo.getMap();
		}
		return null;
	}

	/**
	 * 执行数据库批量更新 字段名和对象属性名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param objects
	 *            模型对象
	 * @return 每条 SQL 更新记录数
	 * @throws SQLException SQL 异常
	 * @throws ReflectiveOperationException 反射异常
	 */
	public int[] batchObject(String sqlText, List<?> objects) throws SQLException, ReflectiveOperationException {
		List<Map<String, ?>> mapList = new ArrayList<Map<String, ?>>();
		for (Object object : objects) {
			mapList.add(TReflect.getMapfromObject(object));
		}
		return this.baseBatch(sqlText, mapList);
	}

	/**
	 * 执行数据库批量更新
	 *
	 * @param sqlText sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param maps 批量处理SQL的参数
	 * @return 每条 SQL 更新记录数
	 * @throws SQLException SQL 异常
	 */
	public int[] batchMap(String sqlText, List<Map<String, ?>> maps) throws SQLException {
		return this.baseBatch(sqlText, maps);
	}
	/**
	 * 调用存储过程,无参数
	 * @param sqlText sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @return 调用结果
	 * @throws SQLException SQL 异常
	 */
	public List<Object> call(String sqlText) throws SQLException {
		return this.baseCall(sqlText,null,null);
	}

	/**
	 *  调用存储过程,map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param sqlText
	 * 				sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param callTypes
	 * 				参数类型 IN,OUT,INOUT 可选
	 * @param maps
	 * 				map参数
	 * @return 调用结果
	 * @throws SQLException SQL 异常
	 */
	public List<Object> call(String sqlText, CallType[] callTypes, Map<String, Object> maps) throws SQLException {
		return this.baseCall(sqlText ,callTypes ,maps);
	}

	/**
	 *  调用存储过程,对象作为参数
	 *
	 * @param sqlText
	 * 				sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param callTypes
	 * 				参数类型 IN,OUT,INOUT 可选
	 * @param arg
	 * 				对象参数
	 * @return 调用结果
	 * @throws SQLException SQL 异常
	 * @throws ReflectiveOperationException 反射异常
	 */
	public List<Object> call(String sqlText, CallType[] callTypes, Object arg) throws SQLException, ReflectiveOperationException {
		if(TReflect.isBasicType(arg.getClass())){
			return call(sqlText, callTypes, arg, null);
		}else{
			Map<String, Object> paramsMap  = TReflect.getMapfromObject(arg);
			return this.baseCall(sqlText,callTypes, paramsMap);
		}

	}

	/**
	 * 调用存储过程,Array作为参数
	 * @param sqlText
	 * 				sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param callTypes
	 * 				参数类型 IN,OUT,INOUT 可选
	 * @param args
	 * 				多个连续参数
	 * @return 调用结果
	 * @throws SQLException SQL 异常
	 */
	public List<Object> call(String sqlText, CallType[] callTypes, Object ... args) throws SQLException {
		Map<String, Object> paramsMap  = TObject.arrayToMap(args);
		return this.baseCall(sqlText,callTypes, paramsMap);
	}


	/**
	 * 关闭连接
	 * @param resultSet 结果集
	 */
	protected static void closeConnection(ResultSet resultSet) {
		try {
			if (resultSet != null) {
				Statement statement = resultSet.getStatement();
				resultSet.close();
				closeConnection(statement);
			}
		} catch (SQLException e) {
			Logger.error(e);
		}

	}

	/**
	 * 关闭连接
	 * @param statement Statement 对象
	 */
	protected static void closeConnection(Statement statement) {
		try {
			if(statement!=null) {
				Connection connection = statement.getConnection();
				statement.close();
				closeConnection(connection);
			}
		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	/**
	 * 关闭连接
	 * @param connection 连接对象
	 */
	private static void closeConnection(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	/**
	 * 关闭结果集
	 * @param resultSet 结果集
	 */
	protected static void closeResult(ResultSet resultSet){
		try {
			if(resultSet!=null) {
				Statement statement = resultSet.getStatement();
				resultSet.close();
				closeStatement(statement);
			}
		} catch (SQLException e) {
			Logger.error(e);
		}
	}

	/**
	 * 关闭 Statement
	 * @param statement Statement 对象
	 */
	protected static void closeStatement(Statement statement){
		try {
			if(statement!=null) {
				statement.close();
			}
		} catch (SQLException e) {
			Logger.error(e);
		}
	}
}

package org.voovan.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.voovan.tools.TReflect;
import org.voovan.tools.TSQL;
import org.voovan.tools.log.Logger;

/**
 * jdbc 操作类
 * 		每个数据库操作函数开机关闭一次连接. 使用:开始来标识参数,例如: Map 和对象形式用:":arg", List 和 Array
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
	 * 内部类,结果集和数据库连接封装
	 * 
	 * @author helyho
	 *
	 */
	public class ResultInfo {
		private ResultSet	resultSet;

		public ResultInfo(ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		public ResultSet getResultSet() {
			return resultSet;
		}

		public <T> List<T> getObjectList(Class<T> t) throws SQLException, ReflectiveOperationException, ParseException {
			@SuppressWarnings("unchecked")
			List<T> objects = (List<T>) TSQL.getAllRowWithObjectList(t, this.resultSet);
			
			// 非事物模式执行
			if (!isTrancation) {
				closeConnection(resultSet);
			}else{
				closeResult(resultSet);
			}
			return objects;
		}

		public List<Map<String, Object>> getMapList() throws SQLException, ReflectiveOperationException {
			List<Map<String, Object>> objects = TSQL.getAllRowWithMapList(this.resultSet);
			
			// 非事物模式执行
			if (!isTrancation) {
				closeConnection(resultSet);
			}else{
				closeResult(resultSet);
			}
			return objects;
		}

		public <T> Object getObject(Class<T> t) throws SQLException, ReflectiveOperationException, ParseException {
			if (resultSet!=null && this.resultSet.next()) {
				@SuppressWarnings("unchecked")
				T obj = (T) TSQL.getOneRowWithObject(t, this.resultSet);
				
				// 非事物模式执行
				if (!isTrancation) {
					closeConnection(resultSet);
				}else{
					closeResult(resultSet);
				}
				return obj;
			} else {
				return null;
			}
		}

		public Map<String, Object> getMap() throws SQLException, ReflectiveOperationException {
			if (resultSet!=null && this.resultSet.next()) {
				Map<String, Object> map = TSQL.getOneRowWithMap(this.resultSet);
				
				// 非事物模式执行
				if (!isTrancation) {
					closeConnection(resultSet);
				}else{
					closeResult(resultSet);
				}
				return map;
			} else {
				return null;
			}
		}

	}

	/**
	 * 构造函数
	 * @param dataSource	数据源
	 * @throws SQLException
	 */
	public JdbcOperate(DataSource dataSource) {
		this.dataSource = dataSource;
		this.isTrancation = false;
	}
	
	/**
	 * 构造函数
	 * @param dataSource	数据源
	 * @param isTrancation   是否启用事物支持
	 * @throws SQLException
	 */
	public JdbcOperate(DataSource dataSource,boolean isTrancation){
		this.dataSource = dataSource;
		this.isTrancation = isTrancation;
	}

	/**
	 * 获取连接
	 * 
	 * @return
	 * @throws SQLException
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
	 * @throws SQLException
	 */
	public void commit() throws SQLException{
		connection.commit();
		closeConnection(connection);
	}
	
	/**
	 * 回滚事物
	 * @throws SQLException
	 */
	public void rollback() throws SQLException{
		connection.rollback();
		closeConnection(connection);
	}
	
	/**
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param mapArg
	 *            参数
	 * @return
	 * @throws SQLException
	 */
	private ResultInfo baseQuery(String sqlText, Map<String, Object> mapArg) throws SQLException {
		Connection conn = getConnection();
		try {
			PreparedStatement preparedStatement = TSQL.createPreparedStatement(conn, sqlText, mapArg);
			ResultSet rs = preparedStatement.executeQuery();
			return new ResultInfo(rs);
		} catch (SQLException e) {
			Logger.error("Query excution SQL Error! \n SQL is : \n\t" + sqlText + ": \n\t " + e.getMessage() + "\n",e);
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
	 * @return
	 * @throws SQLException
	 */
	private int baseUpdate(String sqlText, Map<String, Object> mapArg) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = TSQL.createPreparedStatement(conn, sqlText, mapArg);
			return preparedStatement.executeUpdate();
		} catch (SQLException e) {
			Logger.error("Update excution SQL Error! \n SQL is :\n\t " + sqlText + "\nError is: \n\t" + e.getMessage() + "\n",e);
		} finally {
			// 非事物模式执行
			if (!isTrancation) {
				closeConnection(preparedStatement);
			}else{
				closeStatement(preparedStatement);
			}
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
	 * @return
	 * @throws SQLException
	 */
	private int[] baseBatch(String sqlText, List<Map<String, Object>> mapArgs) throws SQLException {
		Connection conn = getConnection();
		PreparedStatement preparedStatement = null;
		try {

			// 非事物模式执行
			if (!isTrancation) {
				conn.setAutoCommit(false);
			}

			// 获取 SQL 中的参数列表
			List<String> sqlParams = TSQL.getSqlParams(sqlText);
			preparedStatement = (PreparedStatement) conn.prepareStatement(TSQL.preparedSql(sqlText));
			if (mapArgs != null) {
				for (Map<String, Object> magArg : mapArgs) {
					// 用 sqlParams 对照 给 preparestatement 填充参数
					TSQL.setPreparedParams(preparedStatement, sqlParams, magArg);
					preparedStatement.addBatch();
				}
			}
			int[] result = preparedStatement.executeBatch();

			// 非事物模式执行
			if (!isTrancation) {
				conn.commit();
			} else {
				closeStatement(preparedStatement);
			}

			return result;
		} catch (SQLException e) {
			Logger.error("Batch excution SQL Error! \n SQL is : \n\t" + sqlText.toString() + ":\n\t" + e.getMessage() + "\n",e);
		} finally {
			// 非事物模式执行
			if (!isTrancation) {
				closeConnection(preparedStatement);
			}
		}
		return new int[0];
	}

	/**
	 * 执行数据库更新
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @return
	 * @throws SQLException
	 */
	public int update(String sqlText) throws SQLException {
		return this.baseUpdate(sqlText, null);
	}

	/**
	 * 执行数据库更新,Object作为参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param object
	 *            object为参数的对象
	 * @return
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	public int update(String sqlText, Object arg) throws SQLException, ReflectiveOperationException {
		if(arg.getClass().getName().startsWith("java")){
			return update(sqlText, arg, null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			return this.baseUpdate(sqlText, paramsMap);
		}
	}

	/**
	 * 执行数据库更新,Map作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param map
	 *            map为参数的对象
	 * @return
	 * @throws Exception
	 */
	public int update(String sqlText, Map<String, Object> mapArg) throws SQLException {
		return this.baseUpdate(sqlText, mapArg);
	}

	/**
	 * 执行数据库更新,Map作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,例如where id=:1
	 * @param map
	 *            map为参数的对象
	 * @return
	 * @throws SQLException 
	 * @throws Exception
	 */
	public int update(String sqlText, Object... args) throws SQLException {
		Map<String, Object> paramsMap = TSQL.arrayToMap(args);
		return this.baseUpdate(sqlText, paramsMap);
	}

	/**
	 * 查询对象集合,无参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param t
	 *            对象模型
	 * @return
	 * @throws SQLException 
	 * @throws ParseException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t) throws SQLException, ReflectiveOperationException, ParseException{
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo==null){
			return null;
		}
		return (List<T>) resultInfo.getObjectList(t);
	}

	/**
	 * 查询单个对象,Object作为参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws ReflectiveOperationException 
	 * @throws SQLException 
	 * @throws ParseException 
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t, Object arg) throws ReflectiveOperationException, SQLException, ParseException{
		if(arg.getClass().getName().startsWith("java")){
			return queryObjectList(sqlText, t, arg,null);
		}else{
		Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo==null){
			return null;
		}
		return (List<T>) resultInfo.getObjectList(t);
		}
	}

	/**
	 * 查询对象集合,map作为参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型Vector
	 * @return
	 * @throws SQLException 
	 * @throws ParseException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t, Map<String, Object> mapArg) throws SQLException, ReflectiveOperationException, ParseException {
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo==null){
			return null;
		}
		return (List<T>) resultInfo.getObjectList(t);
	}

	/**
	 * 查询对象集合,map作为参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,索引标识从1开始,例如where id=:1
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型Vector
	 * @return
	 * @throws SQLException 
	 * @throws ParseException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t, Object... args) throws SQLException, ReflectiveOperationException, ParseException {
		Map<String, Object> paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo==null){
			return null;
		}
		return (List<T>) resultInfo.getObjectList(t);
	}

	/**
	 * 查询对象集合,无参数
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param t
	 *            对象模型
	 * @return
	 * @throws SQLException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText) throws SQLException, ReflectiveOperationException  {
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo==null){
			return null;
		}
		return resultInfo.getMapList();
	}

	/**
	 * 查询单个对象,Object作为参数
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws ReflectiveOperationException 
	 * @throws SQLException 
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText, Object arg) throws ReflectiveOperationException, SQLException  {
		if(arg.getClass().getName().startsWith("java")){
			return queryMapList(sqlText, arg, null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
			if(resultInfo==null){
				return null;
			}
			return resultInfo.getMapList();
		}
	}

	/**
	 * 查询对象集合,map作为参数
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws SQLException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText, Map<String, Object> mapArg) throws SQLException, ReflectiveOperationException{
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo==null){
			return null;
		}
		return resultInfo.getMapList();
	}

	/**
	 * 查询单个对象,Object作为参数
	 *
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,索引标识从1开始,例如where id=:1
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws SQLException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText, Object... args) throws SQLException, ReflectiveOperationException {
		Map<String, Object> paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo==null){
			return null;
		}
		return resultInfo.getMapList();
	}

	/**
	 * 查询单个对象,无参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param tsqlText
	 *            对象模型
	 * @return
	 * @throws SQLException 
	 * @throws ParseException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t) throws SQLException, ReflectiveOperationException, ParseException  {
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo==null){
			return null;
		}
		return (T) resultInfo.getObject(t);
	}

	/**
	 * 查询单个对象,Object作为参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws ReflectiveOperationException 
	 * @throws SQLException 
	 * @throws ParseException 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Object arg) throws ReflectiveOperationException, SQLException, ParseException {
		if(arg.getClass().getName().startsWith("java")){
			return queryObject(sqlText,t,arg,null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
			if(resultInfo==null){
				return null;
			}
			return (T) resultInfo.getObject(t);
		}
	}

	/**
	 * 查询单个对象,map作为参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws SQLException 
	 * @throws ParseException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Map<String, Object> mapArg) throws SQLException, ReflectiveOperationException, ParseException {
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo==null){
			return null;
		}
		return (T) resultInfo.getObject(t);
	}

	/**
	 * 查询单个对象,map作为参数 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,索引标识从1开始,例如where id=:1
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws ParseException 
	 * @throws ReflectiveOperationException 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Object... args) throws SQLException, ReflectiveOperationException, ParseException {
		Map<String, Object> paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo==null){
			return null;
		}
		return (T) resultInfo.getObject(t);
	}

	/**
	 * 查询单行,返回 Map,无参数
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param tsqlText
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryMap(String sqlText) throws SQLException, ReflectiveOperationException {
		ResultInfo resultInfo = this.baseQuery(sqlText, null);
		if(resultInfo==null){
			return null;
		}
		return resultInfo.getMap();
	}

	/**
	 * 查询单行,返回 Map,Object作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"引导一个标识,例如where id=:id,中 id 就是标识。
	 * @param arg
	 *            arg参数 属性指代SQL字符串的标识,属性值用于在SQL字符串中替换标识。
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryMap(String sqlText, Object arg) throws SQLException, ReflectiveOperationException {
		if(arg.getClass().getName().startsWith("java")){
			return queryMap(sqlText,arg,null);
		}else{
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg);
			ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
			if(resultInfo==null){
				return null;
			}
			return resultInfo.getMap();
		}
	}

	/**
	 * 查询单行,返回 Map,map作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"引导一个标识,例如where id=:id,中 id 就是标识。
	 * @param map
	 *            map参数，key指代SQL字符串的标识,value用于在SQL字符串中替换标识。
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryMap(String sqlText, Map<String, Object> mapArg) throws SQLException, ReflectiveOperationException {
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		if(resultInfo==null){
			return null;
		}
		return resultInfo.getMap();
	}

	/**
	 * 查询单行,返回 Map,Array作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"引导一个索引标识,索引标识从1开始,例如where id=:1
	 * @param args
	 *            args参数
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryMap(String sqlText, Object... args) throws SQLException, ReflectiveOperationException {
		Map<String, Object> paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		if(resultInfo==null){
			return null;
		}
		return resultInfo.getMap();
	}

	/**
	 * 执行数据库批量更新 字段名和对象属性名大消息必须大小写一致
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param objects
	 *            模型对象
	 * @return
	 * @throws SQLException
	 */
	public int[] batchObject(String sqlText, List<Object> objects) throws SQLException, ReflectiveOperationException {
		List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
		for (Object object : objects) {
			mapList.add(TReflect.getMapfromObject(object));
		}
		return this.baseBatch(sqlText, mapList);
	}

	/**
	 * 执行数据库批量更新
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=:id
	 * @param maps
	 *            批量处理SQL的参数
	 * @return
	 * @throws SQLException
	 */
	public int[] batchMap(String sqlText, List<Map<String, Object>> maps) throws SQLException, ReflectiveOperationException {
		return this.baseBatch(sqlText, maps);
	}

	/**
	 * 关闭连接
	 * @param resultSet
	 */
	private static void closeConnection(ResultSet resultSet) {
		Statement statement = null;
		Connection connection = null;
		try {
			statement = resultSet.getStatement();
			connection = statement.getConnection();
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}

		try {
			if (resultSet != null) {
				resultSet.close();
			}
			if (statement != null) {
				statement.close();
			}
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}
	}

	/**
	 * 关闭连接
	 * @param statement
	 */
	private static void closeConnection(Statement statement) {
		Connection connection = null;
		try {
			connection = statement.getConnection();
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}

		try {
			if (statement != null) {
				statement.close();
			}
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}
	}
	
	/**
	 * 关闭连接
	 * @param connection
	 */
	private static void closeConnection(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}
	}
	
	/**
	 * 关闭结果集
	 * @param resultSet
	 */
	private static void closeResult(ResultSet resultSet){
		Statement statement = null;
		try {
			statement = resultSet.getStatement();
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}
		try {
			if (statement != null) {
				statement.close();
			}
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}
	}
	
	/**
	 * 关闭 Statement
	 * @param statement
	 */
	private static void closeStatement(Statement statement){
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			Logger.error(e.getMessage(),e);
		}
	}
}

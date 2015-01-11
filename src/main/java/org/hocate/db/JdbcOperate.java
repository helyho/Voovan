package org.hocate.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.hocate.tools.TReflect;
import org.hocate.tools.TSQL;

/**
 * jdbc 操作类,每个数据库操作函数开机关闭一次连接.
 * 使用:开始来标识参数,例如: Map 和对象形式用:":arg", List 和 Array 形式用":1"
 * @author helyho
 *
 */
public class JdbcOperate {

	private static Logger logger = Logger.getLogger(JdbcOperate.class);
	private DataSource dataSource;

	/**
	 * 内部类,结果集和数据库连接封装
	 * @author helyho
	 *
	 */
	public class ResultInfo {
		private ResultSet resultSet;

		public ResultInfo(ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		public ResultSet getResultSet() {
			return resultSet;
		}
		
		public <T> List<T> getObjectList(Class<T> t) throws Exception {
			@SuppressWarnings("unchecked")
			List<T> objects = (List<T>) TSQL.getAllRowWithObjectList(t,this.resultSet);
			closeConnection(resultSet);
			return objects;
		}
		
		public List<Map<String, Object>> getMapList() throws Exception{
			List<Map<String, Object>> objects = TSQL.getAllRowWithMapList(this.resultSet);
			closeConnection(resultSet);
			return objects;
		}
		
		public <T> Object getObject(Class<T> t) throws Exception{
			if (this.resultSet.next()) {
				@SuppressWarnings("unchecked")
				T obj = (T) TSQL.getOneRowWithObject(t, this.resultSet);
				closeConnection(resultSet);
				return obj;
			} else {
				return null;
			}
		}
		
		public Map<String, Object> getMap() throws Exception{
			if(this.resultSet.next()){
			Map<String, Object> map = TSQL.getOneRowWithMap(this.resultSet);
			closeConnection(resultSet);
			return map;
			}
			else {
				return null;
			}
		}

	}

	public JdbcOperate(DataSource dataSource) throws SQLException {
		this.dataSource = dataSource;
	}

	/**
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param mapArg    参数
	 * @return
	 * @throws SQLException
	 */
	private ResultInfo baseQuery(String sqlText,Map<String, Object> mapArg) throws SQLException {
		Connection conn = dataSource.getConnection();
		try {
			PreparedStatement preparedStatementar = TSQL.createPreparedStatement(conn,sqlText,mapArg);
			ResultSet rs = preparedStatementar.executeQuery();
			return new ResultInfo(rs);
		} catch (Exception e) {
			throw new SQLException("Excution SQL Error! \n SQL is : \n\t"
					+ sqlText + "\n Error is:\n\t" + e.getMessage() + "\n");
		}
	}

	/**
	 * 执行数据库更新
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param mapArg   参数
	 * @return
	 * @throws SQLException
	 */
	private int baseUpdate(String sqlText,Map<String, Object> mapArg) throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement preparedStatementar = null;
		try {
			preparedStatementar = TSQL.createPreparedStatement(conn,sqlText,mapArg);
			int result = preparedStatementar.executeUpdate();
			return result;
		} catch (Exception e) {
			throw new SQLException("Excution SQL Error! \n SQL is :\n\t "
					+ sqlText + "\nError is:\n\t" + e.getMessage() + "\n");
		} finally {
			closeConnection(preparedStatementar);
		}
	}

	/**
	 * 执行数据库批量更新
	 * @param sqlText
	 * 			sql字符串
	 * @param mapArgs   参数
	 * @return
	 * @throws SQLException
	 */
	private int[] baseBatch(String sqlText,List<Map<String, Object>> mapArgs) throws SQLException {
		Connection conn = dataSource.getConnection();
		PreparedStatement preparedStatement = null;
		try {
			conn.setAutoCommit(false);
			logger.info("Executed: \n" + sqlText);
			//获取 SQL 中的参数列表
			List<String> sqlParams = TSQL.getSqlParams(sqlText);
			preparedStatement = (PreparedStatement) conn.prepareStatement(TSQL.preparedSql(sqlText));
			if(mapArgs!=null){
				for(Map<String, Object> magArg : mapArgs){
					//用 sqlParams 对照 给 preparestatement 填充参数
					TSQL.setPreparedParams(preparedStatement,sqlParams,magArg);
					preparedStatement.addBatch();
				}
			}
			int[] result =preparedStatement.executeBatch();
			conn.commit();
			return result;
		} catch (Exception e) {
			throw new SQLException("Excution SQL Error! \n SQL is : \n\t"
					+ sqlText.toString() + "\n Error is:\n\t" + e.getMessage()
					+ "\n");
		} finally {
			closeConnection(preparedStatement);
		}
	}

	/**
	 * 执行数据库更新
	 * @param sqlText
	 * 			sql字符串 参数使用":"作为标识,例如where id=:id
	 * @return
	 * @throws SQLException
	 */
	public int update(String sqlText) throws SQLException {
		return this.baseUpdate(sqlText,null);
	}

	/**
	 * 执行数据库更新,Object作为参数
	 * 			字段名和对象属性名大消息必须大小写一致
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param object
	 *            object为参数的对象
	 * @return
	 * @throws Exception
	 */
	public int update(String sqlText, Object arg) throws Exception {
		Map<String, Object>paramsMap = TReflect.getMapfromObject(arg);
		int result = this.baseUpdate(sqlText,paramsMap);
		return result;
	}

	/**
	 * 执行数据库更新,Map作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param map
	 *            map为参数的对象
	 * @return
	 * @throws Exception
	 */
	public int update(String sqlText, Map<String, Object> mapArg) throws Exception {
		int result = this.baseUpdate(sqlText,mapArg);
		return result;
	}
	
	/**
	 * 执行数据库更新,Map作为参数
	 * 	
	 * @param sqlText
	 *            sql字符串 参数使用":"作为索引标识,例如where id=:1
	 * @param map
	 *            map为参数的对象
	 * @return
	 * @throws Exception
	 */
	public int update(String sqlText, Object ...args) throws Exception {
		Map<String, Object>paramsMap = TSQL.arrayToMap(args);
		int result = this.baseUpdate(sqlText,paramsMap);
		return result;
	}
	
	/**
	 * 查询对象集合,无参数
	 * 			字段名和对象属性名大消息必须大小写一致
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t)
			throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText,null);
		return (List<T>) resultInfo.getObjectList(t);
	}

	/**
	 * 查询单个对象,Object作为参数
	 * 			字段名和对象属性名大消息必须大小写一致
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t, Object arg)
			throws Exception {
		Map<String, Object>paramsMap = TReflect.getMapfromObject(arg);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
		return (List<T>) resultInfo.getObjectList(t);
	}

	/**
	 * 查询对象集合,map作为参数
	 * 			字段名和对象属性名大消息必须大小写一致
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型Vector
	 * @return
	 * @throws Exception
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t,
			Map<String, Object> mapArg) throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText, mapArg);
		return (List<T>) resultInfo.getObjectList(t);
	}

	/**
	 * 查询对象集合,map作为参数
	 *		字段名和对象属性名大消息必须大小写一致
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为索引标识,索引标识从1开始,例如where id=:1
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型Vector
	 * @return
	 * @throws Exception
	 */
	public <T> List<T> queryObjectList(String sqlText, Class<T> t,
			Object... args) throws Exception {
		Map<String, Object>paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText, paramsMap);
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
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText)
			throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText,null);
		return resultInfo.getMapList();
	}

	/**
	 * 查询单个对象,Object作为参数
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText, Object arg)
			throws Exception {
		Map<String, Object>paramsMap = TReflect.getMapfromObject(arg);
		ResultInfo resultInfo = this.baseQuery(sqlText,paramsMap);
		return resultInfo.getMapList();
	}

	/**
	 * 查询对象集合,map作为参数
	 * 
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText,
			Map<String, Object> mapArg) throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText,mapArg);
		return resultInfo.getMapList();
	}

	/**
	 * 查询单个对象,Object作为参数 
	 *
	 * @param <T>
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为索引标识,索引标识从1开始,例如where id=:1
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, Object>> queryMapList(String sqlText,
			Object... args) throws Exception {
		Map<String, Object>paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText,paramsMap);
		return resultInfo.getMapList();
	}

	/**
	 * 查询单个对象,无参数
	 * 		字段名和对象属性名大消息必须大小写一致
	 * @param sqlText
	 *            sql字符串
	 * @param tsqlText
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t) throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText,null);
		return (T) resultInfo.getObject(t);
	}

	/**
	 * 查询单个对象,Object作为参数
	 * 			字段名和对象属性名大消息必须大小写一致
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param param
	 *            Object参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Object arg)
			throws Exception {
		Map<String, Object>paramsMap = TReflect.getMapfromObject(arg);
		ResultInfo resultInfo = this.baseQuery(sqlText,paramsMap);
		return (T) resultInfo.getObject(t);
	}

	/**
	 * 查询单个对象,map作为参数
	 * 			字段名和对象属性名大消息必须大小写一致
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Map<String, Object> mapArg)
			throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText,mapArg);
		return (T) resultInfo.getObject(t);
	}

	/**
	 * 查询单个对象,map作为参数
	 *		字段名和对象属性名大消息必须大小写一致
	 * @param sqlText
	 *            sql字符串 参数使用":"作为索引标识,索引标识从1开始,例如where id=:1
	 * @param map
	 *            map参数
	 * @param t
	 *            对象模型
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public <T> T queryObject(String sqlText, Class<T> t, Object... args)
			throws Exception {
		Map<String, Object>paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText,paramsMap);
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
	public Map<String, Object> queryMap(String sqlText) throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText,null);
		return resultInfo.getMap();
	}

	/**
	 * 查询单行,返回 Map,Object作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param param
	 *            Object参数
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryMap(String sqlText, Object arg)
			throws Exception {
		Map<String, Object>paramsMap = TReflect.getMapfromObject(arg);
		ResultInfo resultInfo = this.baseQuery(sqlText,paramsMap);
		return resultInfo.getMap();
	}

	/**
	 * 查询单行,返回 Map,map作为参数
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param map
	 *            map参数
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryMap(String sqlText, Map<String, Object> mapArg)
			throws Exception {
		ResultInfo resultInfo = this.baseQuery(sqlText,mapArg);
		return resultInfo.getMap();
	}

	/**
	 * 查询单行,返回 Map,Array作为参数 
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为索引标识,索引标识从1开始,例如where id=:1
	 * @param param
	 *            Object参数
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> queryMap(String sqlText, Object... args)
			throws Exception {
		Map<String, Object>paramsMap = TSQL.arrayToMap(args);
		ResultInfo resultInfo = this.baseQuery(sqlText,paramsMap);
		return resultInfo.getMap();
	}

	/**
	 * 执行数据库批量更新
	 * 			字段名和对象属性名大消息必须大小写一致
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param objects
	 *            模型对象
	 * @return
	 * @throws SQLException
	 */
	public int[] batchObject(String sqlText, List<Object> objects)
			throws Exception {
		ArrayList<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
		for(Object object: objects){
			mapList.add(TReflect.getMapfromObject(object));
		}
		int[] result = this.baseBatch(sqlText,mapList);
		return result;
	}

	/**
	 * 执行数据库批量更新
	 * 
	 * @param sqlText
	 *            sql字符串 参数使用":"作为标识,例如where id=:id
	 * @param maps
	 *            批量处理SQL的参数
	 * @return
	 * @throws SQLException
	 */
	public int[] batchMap(String sqlText, List<Map<String, Object>> maps)
			throws Exception {
		return this.baseBatch(sqlText,maps);
	}
	
	private static void closeConnection(ResultSet resultSet){
		Statement statement = null;
		Connection connection = null;
		try {
			statement = resultSet.getStatement();
			connection = statement.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}
	
	private static void closeConnection(Statement statement){
		Connection connection = null;
		try {
			connection = statement.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try {
			if (statement != null) {
				statement.close();
			}
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}

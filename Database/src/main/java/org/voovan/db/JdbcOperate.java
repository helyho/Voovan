package org.voovan.db;

import org.voovan.db.exception.UpdateCountException;
import org.voovan.tools.TObject;
import org.voovan.tools.TSQL;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * jdbc 操作类
 * 		每个数据库操作函数 开启关闭一次连接. 使用:开始来标识参数,例如: Map 和对象形式用:":arg", List 和 Array 形式用":1"
 * 		所有行为均为非线程安全
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JdbcOperate implements Closeable {
	private static Map<Long, JdbcOperate> JDBC_OPERATE_THREAD_LIST = new ConcurrentHashMap<Long, JdbcOperate>();

	private DataSource	dataSource;
	private Connection	connection;
	private TranscationType transcationType;
	private Savepoint savepoint = null;
	private Statement statement;
	private ResultSet resultSet;

	private DataBaseType dataBaseType;

	private List<JdbcOperate> bindedJdbcOperate = new ArrayList<JdbcOperate>();
	private boolean isTransactionFinished = false;

	/**
	 * 构造函数
	 * @param dataSource	数据源
	 */
	public JdbcOperate(DataSource dataSource) {
		this.dataSource = dataSource;
		this.dataBaseType = TSQL.getDataBaseType(dataSource);
		this.transcationType = TranscationType.NONE;
	}

	/**
	 * 构造函数
	 * @param dataSource	数据源
	 * @param isTrancation   是否启用事物支持, 默认使用的`NEST`(嵌套事务)的事物方式
	 */
	public JdbcOperate(DataSource dataSource, boolean isTrancation){
		this.dataSource = dataSource;
		this.dataBaseType = TSQL.getDataBaseType(dataSource);
		this.transcationType = (isTrancation? TranscationType.ALONE : TranscationType.NONE);
	}

	/**
	 * 构造函数
	 * @param dataSource	数据源
	 * @param transcationType    是否启用事务支持, 设置事务模式
	 */
	public JdbcOperate(DataSource dataSource, TranscationType transcationType){
		this.dataSource = dataSource;
		this.dataBaseType = TSQL.getDataBaseType(dataSource);
		this.transcationType = transcationType;
	}

	/**
	 * 获取数据库类型g
	 * @return DataBaseType数据库类型
	 */
	public DataBaseType getDataBaseType() {
		return dataBaseType;
	}

	/**
	 * 增加连接事务关联绑定
	 * 		只能绑定 TranscationType 为 NEST 的事务
	 * @param subJdbcOperate 连接操作对象
	 * @param bothway true: 双向绑定, false: 单向绑定,只建立当前连接和目标连接的绑定关系
	 *                双向绑定意味这任意一个节点的 commit 或者 rollback 操作都会直接联动所有的事物同时操作(提交/回滚), 如果是单向绑定,只会联动下级事物同时操作(提交/回滚)
	 * @return true: 增加连接事务绑定成功, false: 增加连接事务绑定失败
	 */
	public synchronized boolean addBind(JdbcOperate subJdbcOperate, boolean bothway){
		if(subJdbcOperate.transcationType == TranscationType.NEST) {
			if(!bindedJdbcOperate.contains(subJdbcOperate)) {
				if(bothway) {
					subJdbcOperate.bindedJdbcOperate.add(this);
				}
				return bindedJdbcOperate.add(subJdbcOperate);
			}
		}

		return false;
	}

	/**
	 * 移除连接事务绑定
	 * @param subJdbcOperate 连接操作对象
	 * @param bothway true: 双向绑定, 解除当前连接和目标连接两个连接相互的绑定关系, : 单向绑定, 只解除当前连接和目标连接的绑定关系
	 * @return true: 移除连接事务绑定成功, false: 移除连接事务绑定失败
	 */
	public synchronized boolean removeBind(JdbcOperate subJdbcOperate, boolean bothway){
		if(bothway) {
			subJdbcOperate.bindedJdbcOperate.remove(this);
		}
		return bindedJdbcOperate.remove(subJdbcOperate);
	}

	/**
	 * 获取连接
	 *
	 * @return 获取数据库连接
	 * @throws SQLException SQL 异常
	 */
	public synchronized Connection getConnection() throws SQLException {
		long threadId = Thread.currentThread().getId();
		//如果连接不存在,或者连接已关闭则重取一个连接
		if (connection == null || connection.isClosed()) {
			//事务嵌套模式
			if (transcationType == TranscationType.NEST) {
				//判断是否有上层事务
				if (JDBC_OPERATE_THREAD_LIST.containsKey(threadId)) {
					connection = JDBC_OPERATE_THREAD_LIST.get(threadId).connection;
					savepoint = connection.setSavepoint();
				} else {
					connection = dataSource.getConnection();
					connection.setAutoCommit(false);
					JDBC_OPERATE_THREAD_LIST.put(threadId, this);
				}
			}
			//孤立事务模式
			else if (transcationType == TranscationType.ALONE){
				connection = dataSource.getConnection();
				connection.setAutoCommit(false);
			}
			//非事务模式
			else if (transcationType == TranscationType.NONE){
				connection = dataSource.getConnection();
				connection.setAutoCommit(true);
			}
		}
		return connection;
	}

	/**
	 * 开启事务
	 * @return true: 开启了一个新的事务, false: 开启了一个 savepoint
	 * @throws SQLException SQL 异常
	 */
	public boolean beginTransaction() throws SQLException {
		long threadId = Thread.currentThread().getId();

		if(connection == null) {
			connection = getConnection();
		}

		if (transcationType!=TranscationType.NONE) {
			savepoint = connection.setSavepoint();
			return false;
		} else {
			transcationType = TranscationType.ALONE;
			connection.setAutoCommit(false);
			JDBC_OPERATE_THREAD_LIST.put(threadId, this);
			return true;
		}
	}

	/**
	 * 返回当前事务形式
	 * @return 事务类型
	 */
	public TranscationType getTranscationType() {
		return transcationType;
	}

	/**
	 * 设置当前事务类型
	 * @param transcationType TranscationType 对象事务类型
	 * @return JdbcOperate 对象
	 */
	public JdbcOperate setTranscationType(TranscationType transcationType) {
		this.transcationType = transcationType;
		return this;
	}

	/**
	 * 提交事务
	 * @param isClose 是否关闭数据库连接
	 * @throws SQLException SQL 异常
	 */
	public synchronized void commit(boolean isClose) throws SQLException {
		if(connection==null){
			return ;
		}

		//关联事务提交
		for(JdbcOperate bindJdbcOperate : bindedJdbcOperate){
			if(this.equals(bindJdbcOperate)) {
				if(!bindJdbcOperate.isTransactionFinished) {
					bindJdbcOperate.commit(isClose);
				}
			}
		}

		if(!connection.isClosed()) {
			connection.commit();

			if (isClose) {
				try {
					close();
				} catch (IOException e) {
					throw new SQLException(e);
				}
			}
		}
		isTransactionFinished = true;
	}

	/**
	 * 回滚事务
	 * @param isClose 是否关闭数据库连接
	 * @throws SQLException SQL 异常
	 */
	public synchronized void rollback(boolean isClose) throws SQLException {
		if(connection==null){
			return ;
		}

		//有事务点则为: 子事务, 无事务点则为: 主事务
		//主事务回滚并可关闭连接
		//子事务回滚事务点, 并不可关闭连接
		if(savepoint!=null) {
			connection.rollback(savepoint);
		} else {
			//关联事务回滚
			for(JdbcOperate bindJdbcOperate : bindedJdbcOperate){
				if(this.equals(bindJdbcOperate)) {
					if(!bindJdbcOperate.isTransactionFinished) {
						bindJdbcOperate.rollback(isClose);
					}
				}
			}

			if(!connection.isClosed()) {
				connection.rollback();

				if (isClose) {
					try {
						close();
					} catch (IOException e) {
						throw new SQLException(e);
					}
				}
			}
			isTransactionFinished = true;
		}
	}

	/**
	 * 提交事务不关闭连接
	 * @throws SQLException SQL 异常
	 */
	public void commit() throws SQLException {
		commit(true);
	}

	/**
	 * 回滚事务不关闭连接
	 * @throws SQLException SQL 异常
	 */
	public void rollback() throws SQLException {
		rollback(true);
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
		SQLException exception = null;
		try {
			//构造PreparedStatement
			PreparedStatement preparedStatement = TSQL.createPreparedStatement(conn, sqlText, mapArg);
			//执行查询
			resultSet = preparedStatement.executeQuery();
			return new ResultInfo(resultSet,this);
		} catch (SQLException e) {
			closeConnection(conn);
			Logger.error("Query execution SQL Error! \n SQL is : \n\t" + sqlText + ": \n\t " ,e);
			exception = e;
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
			statement = preparedStatement;
			return preparedStatement.executeUpdate();
		} catch (SQLException e) {
			Logger.error("Update execution SQL Error! \n SQL is :\n\t " + sqlText + "\nError is: \n\t" ,e);
			exception = e;
		} finally {
			// 非事务模式执行
			if (transcationType == TranscationType.NONE) {
				closeConnection(preparedStatement);
			} else {
				if(exception!=null) {
					rollback();
					closeStatement(preparedStatement);
				}
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
	 * @param sqlTexts
	 *            sql字符串
	 * @return 每条 SQL 更新记录数
	 * @throws SQLException SQL 异常
	 */
	private int[] baseBatch(String[] sqlTexts) throws SQLException {
		if(sqlTexts.length == 0) {
			return new int[0];
		}

		Connection conn = getConnection();
		Statement statement = null;
		SQLException exception = null;
		try {

			statement = (Statement) conn.createStatement();

			for(String sqlText : sqlTexts) {
				statement.addBatch(sqlText);

				if (Logger.isLogLevel("DEBUG")) {
					Logger.sql("[SQL_Executed]: " + sqlText);
				}
			}

			int[] result = statement.executeBatch();

			return result;
		} catch (SQLException e) {
			Logger.error("Batch execution SQL Error! \n SQL is : \n\t" + JSON.toJSON(sqlTexts) + ":\n\t" ,e);
			exception = e;
		} finally {
			// 非事务模式执行
			if (transcationType == TranscationType.NONE) {
				closeConnection(statement);
			} else {
				if(exception!=null) {
					rollback();
					closeStatement(statement);
				}
			}
		}

		if(exception!=null){
			throw exception;
		}

		return new int[0];
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
	private int[] baseBatch(String sqlText, Collection<Map<String, Object>> mapArgs) throws SQLException {
		if(mapArgs.size() == 0) {
			return new int[0];
		}

		Connection conn = getConnection();
		PreparedStatement preparedStatement = null;
		SQLException exception = null;
		try {

			// 获取 SQL 中的参数列表
			List<String> sqlParams = TSQL.getSqlParamNames(sqlText);
			preparedStatement = (PreparedStatement) conn.prepareStatement(TSQL.preparedSql(sqlText));
			if (mapArgs != null) {
				for (Map<String, Object> magArg : mapArgs) {
					// 用 sqlParams 对照 给 preparestatement 填充参数
					TSQL.setPreparedParams(preparedStatement, sqlParams, magArg);
					preparedStatement.addBatch();
				}
			}

			if(Logger.isLogLevel("DEBUG")) {
				Logger.sql("[SQL_Executed]: " + sqlText);
			}

			statement = preparedStatement;
			int[] result = preparedStatement.executeBatch();

			return result;
		} catch (SQLException e) {
			Logger.error("Batch execution SQL Error! \n SQL is : \n\t" + sqlText + ":\n\t" ,e);
			exception = e;
		} finally {
			// 非事务模式执行
			if (transcationType == TranscationType.NONE) {
				closeConnection(preparedStatement);
			} else {
				if(exception!=null) {
					rollback();
					closeStatement(preparedStatement);
				}
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
			statement = callableStatement;
			callableStatement.executeUpdate();
			List<Object> objList = TSQL.getCallableStatementResult(callableStatement);
			return objList;
		} catch (SQLException e) {
			Logger.error("Query execution SQL Error! \n SQL is : \n\t" + sqlText + ": \n\t " ,e);
			exception = e;
		} finally {
			// 非事务模式执行
			if (transcationType == TranscationType.NONE) {
				closeConnection(callableStatement);
			} else {
				if(exception!=null) {
					rollback();
					closeStatement(callableStatement);
				}
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
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg, true);
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
	 * 执行数据库更新
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param updateCount 期望的更新记录数, 如果不符合会抛出异常
	 * @return 更新记录数
	 * @throws SQLException SQL 异常
	 */
	public int updateAndCheck(String sqlText, int updateCount) throws SQLException {
		int count = update(sqlText);
		if(count!=updateCount){
			throw new UpdateCountException("Update row count error, expect: " + updateCount + "actual: " + count + ".");
		}

		return count;
	}

	/**
	 * 执行数据库更新,Object作为参数 字段名和对象属性名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param updateCount 期望的更新记录数, 如果不符合会抛出异常
	 * @param arg
	 *            object为参数的对象
	 * @return SQL 异常
	 * @throws ReflectiveOperationException 反射异常
	 * @throws SQLException SQL 异常
	 */
	public int updateAndCheck(String sqlText, int updateCount, Object arg) throws SQLException, ReflectiveOperationException {
		int count = update(sqlText, arg);
		if(count!=updateCount){
			throw new UpdateCountException("Update row count error, expect: " + updateCount + "actual: " + count + ".");
		}

		return count;
	}

	/**
	 * 执行数据库更新,Map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为标识,例如where id=::id
	 * @param updateCount 期望的更新记录数, 如果不符合会抛出异常
	 * @param mapArg
	 *            map为参数的对象
	 * @return 更新记录数
	 * @throws SQLException SQL 异常
	 */
	public int updateAndCheck(String sqlText, int updateCount, Map<String, Object> mapArg) throws SQLException {
		int count = update(sqlText, mapArg);
		if(count!=updateCount){
			throw new UpdateCountException("Update row count error, expect: " + updateCount + "actual: " + count + ".");
		}

		return count;
	}

	/**
	 * 执行数据库更新,Map作为参数,字段名和Map键名大消息必须大小写一致
	 *
	 * @param sqlText
	 *            sql字符串 参数使用"::"作为索引标识,引导一个索引标识,索引标识从1开始,例如where id=::1
	 * @param updateCount 期望的更新记录数, 如果不符合会抛出异常
	 * @param args
	 *            多参数
	 * @return 更新记录数
	 * @throws SQLException  SQL 异常
	 */
	public int updateAndCheck(String sqlText, int updateCount, Object... args) throws SQLException {
		Map<String, Object> paramsMap = TObject.arrayToMap(args);
		int count = update(sqlText, paramsMap);
		if(count!=updateCount){
			throw new UpdateCountException("Update row count error, expect: " + updateCount + "actual: " + count + ".");
		}

		return count;
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
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg, true);
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
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg, true);
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
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg, true);
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
			Map<String, Object> paramsMap = TReflect.getMapfromObject(arg, true);
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
	public int[] batchObject(String sqlText, Collection<?> objects) throws SQLException, ReflectiveOperationException {
		List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
		for (Object object : objects) {
			mapList.add(TReflect.getMapfromObject(object, true));
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
	public int[] batchMap(String sqlText, Collection<Map<String, Object>> maps) throws SQLException {
		return this.baseBatch(sqlText, maps);
	}

	/**
	 * 执行数据库批量更新
	 *
	 * @param sqlTexts
	 *            sql字符串
	 * @return 每条 SQL 更新记录数
	 * @throws SQLException SQL 异常
	 */
	public int[] batch(String[] sqlTexts) throws SQLException {

		return this.baseBatch(sqlTexts);
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
			Map<String, Object> paramsMap  = TReflect.getMapfromObject(arg, true);
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
				JDBC_OPERATE_THREAD_LIST.remove(Thread.currentThread().getId());
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


	@Override
	public void close() throws IOException {
		try {
			if (resultSet != null && !resultSet.isClosed()) {
				closeConnection(resultSet);
			} else if (statement != null && !statement.isClosed()) {
				closeConnection(statement);
			} else {
				closeConnection(connection);
			}
		} catch (Exception e){
			throw new IOException(e);
		} finally {
			this.resultSet = null;
			this.statement = null;
			this.connection = null;
		}
	}
}

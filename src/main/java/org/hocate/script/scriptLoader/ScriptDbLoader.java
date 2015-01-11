package org.hocate.script.scriptLoader;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.sql.DataSource;

import org.hocate.db.JdbcOperate;
import org.hocate.script.ScriptEntity;
import org.hocate.tools.TObject;

/**
 * 从数据库获得脚本对象实体
 * 		为提高效率脚本对象实体为单例模式
 * @author helyho
 *
 */
public class ScriptDbLoader implements ScriptLoader{
	
	private DataSource dataSource;
	
	/**
	 * 构造函数
	 * @param dataSource 数据源
	 */
	public ScriptDbLoader(DataSource dataSource){
		this.dataSource = dataSource;
	}
	
	@Override
	public synchronized ScriptEntity getScriptEntity(String packagePath, float version) {
		try {
			JdbcOperate jdbOperation = new JdbcOperate(dataSource);
			String sqlString = "SELECT id,packagePath,version,canReload,sourcePath,createDate FROM sc_script "
										+ "WHERE packagePath=:1 and version = :2 AND state=1";
			ScriptEntity se = TObject.cast( 
					jdbOperation.queryObject(sqlString, ScriptEntity.class,packagePath,version)
					);
			return se;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public synchronized ScriptEntity getScriptEntity(String packagePath) {
		try {
			JdbcOperate jdbOperation = new JdbcOperate(dataSource);
			String sqlStr = "SELECT id,packagePath,version,canReload,sourcePath,createDate \n" +
							"  FROM sc_script \n" + 
							"where (packagePath, version) in" + 
							"      (select packagePath, max(version) from sc_script WHERE packagePath=:1 AND state=1 group by packagePath )";
			 
			ScriptEntity se = TObject.cast( 
					jdbOperation.queryObject(sqlStr, ScriptEntity.class,new Object[]{packagePath})
					);
				return se;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<String> packagePaths() {
		Vector<String>packagePaths = new Vector<String>();
		try {
			JdbcOperate jdbOperation = new JdbcOperate(dataSource);
			String sqlStr = "SELECT distinct packagePath  FROM sc_script WHERE state=1" ;
			List<Map<String, Object>> listMap = jdbOperation.queryMapList(sqlStr);
			for(Map<String, Object> map : listMap){
				packagePaths.add(map.get("packagePath").toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return packagePaths;
	}

	@Override
	public synchronized List<ScriptEntity> scriptEntitys() {
		List<ScriptEntity>scriptEntitys = new Vector<ScriptEntity>();
		try {
			JdbcOperate jdbOperation = new JdbcOperate(dataSource);
			String sqlString = " select id,packagePath,version,canReload,sourcePath,createDate " +
								 "  from sc_script" + 
								 " where (packagePath, version) in" + 
								 "       (select packagePath, max(version) from sc_script WHERE state=1 group by packagePath )";
			scriptEntitys = jdbOperation.queryObjectList(sqlString, ScriptEntity.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return scriptEntitys;
	}
	

	@Override
	public void reLoad() {
		for(ScriptEntity scripeEntity:scriptEntitys()){
			scripeEntity.reloadInfo(this);
		}
	}

	

}

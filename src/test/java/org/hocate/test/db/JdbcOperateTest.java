package org.hocate.test.db;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hocate.db.JdbcOperate;
import org.hocate.tools.log.Logger;
import org.hocate.tools.TEnv;
import org.hocate.tools.TObject;
import org.hocate.tools.TProperties;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

public class JdbcOperateTest {
	public static void main(String[] args) throws Exception {
		DruidDataSource dataSource = null;
		File propertiesFile = new File(TEnv.getSysPathFromContext("Config" + File.separator + "config.properties"));
		String druidPath = TEnv.getSysPathFromContext("Config" + File.separator
				+ TProperties.getString(propertiesFile, "Database.Druid"));
		try {
			Properties druidProperites = TProperties.getProperties(new File(druidPath));
		    dataSource = TObject.cast(DruidDataSourceFactory.createDataSource(druidProperites));
			dataSource.init();
			Logger.info("Database connection pool init finished");
		} catch (Exception e) {
			Logger.error(e);
		}
		
		JdbcOperate jOperate = new JdbcOperate(dataSource);
		
		List<Map<String,Object>> smm = jOperate.queryMapList("select * from sc_script");
		Logger.info(smm);
		
		//Map 参数 => List<Map>
		HashMap<String, Object> xMap = new HashMap<String, Object>();
		xMap.put("packagePath", "org.hocate.test");
		List<Map<String,Object>> mm = jOperate.queryMapList("select * from sc_script where PackagePath=:packagePath",xMap);
		Logger.info(mm);
		
		//对象参数 => List<Object>
		ScriptEntity sEntity = new ScriptEntity();
		sEntity.setPackagePath("org.hocate.test");
		List<ScriptEntity> lmm = jOperate.queryObjectList("select * from sc_script where PackagePath=:packagePath",ScriptEntity.class,sEntity);
		Logger.info(lmm);
		
		//不定个数参数 => Object
		ScriptEntity llmm = jOperate.queryObject("select * from sc_script where PackagePath=:1 and version=:2",ScriptEntity.class,"org.hocate.test",2.0);
		Logger.info(llmm);
		
		//事物测试
		jOperate = new JdbcOperate(dataSource,true);
		Logger.info(jOperate.update("update sc_script set version=0"));
		Logger.info(jOperate.queryMapList("select * from sc_script"));
		jOperate.rollback();
	}
}

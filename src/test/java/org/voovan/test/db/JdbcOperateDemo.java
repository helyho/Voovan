package org.voovan.test.db;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.voovan.db.JdbcOperate;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.log.Logger;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

public class JdbcOperateDemo {
	public static void main(String[] args) throws Exception {
		DruidDataSource dataSource = null;
		try {
			String druidPath = TEnv.getSystemPath("Config" + File.separator + "datasource.properties");
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
		
		//Map参数 => 返回List<Map>
		HashMap<String, Object> xMap = new HashMap<String, Object>();
		xMap.put("packagePath", "org.hocate.test");
		List<Map<String,Object>> mm = jOperate.queryMapList("select * from sc_script where PackagePath=::packagePath",xMap);
		Logger.info(mm);
		
		//对象参数 => 返回对象列表 List<Object>
		ScriptEntity sEntity = new ScriptEntity();
		sEntity.setPackagePath("org.hocate.test");
		List<ScriptEntity> lmm = jOperate.queryObjectList("select * from sc_script where PackagePath=::packagePath",ScriptEntity.class,sEntity);
		Logger.info(lmm);
		
		//不定个数参数 => 返回一个Object
		ScriptEntity llmm = jOperate.queryObject("select * from sc_script where PackagePath=::1 and version=::2",ScriptEntity.class,"org.hocate.test",2.0);
		Logger.info(llmm);
		
		//事物测试
		jOperate = new JdbcOperate(dataSource,true);
		Logger.info(jOperate.update("update sc_script set version=0"));
		Logger.info(jOperate.queryMapList("select * from sc_script"));
		jOperate.rollback();
	}
}

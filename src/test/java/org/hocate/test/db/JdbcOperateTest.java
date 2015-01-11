package org.hocate.test.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hocate.biz.config.ConfigLoader;
import org.hocate.db.JdbcOperate;
import org.hocate.script.ScriptEntity;

public class JdbcOperateTest {
	private static Logger logger = Logger.getLogger(JdbcOperateTest.class);
	public static void main(String[] args) throws Exception {
		ConfigLoader cl = new ConfigLoader();
		JdbcOperate jOperate = new JdbcOperate(cl.getDataSource());
		
		Map<String,Object> smm = jOperate.queryMap("select * from sc_script");
		logger.info(smm);
		
		//Map 参数 => List<Map>
		HashMap<String, Object> xMap = new HashMap<String, Object>();
		xMap.put("packagePath", "org.hocate.test");
		List<Map<String,Object>> mm = jOperate.queryMapList("select * from sc_script where PackagePath=:packagePath",xMap);
		logger.info(mm);
		
		//对象参数 => List<Object>
		ScriptEntity sEntity = new ScriptEntity();
		sEntity.setPackagePath("org.hocate.test");
		List<ScriptEntity> lmm = jOperate.queryObjectList("select * from sc_script where PackagePath=:packagePath",ScriptEntity.class,sEntity);
		logger.info(lmm);
		
		//不定个数参数 => Object
		ScriptEntity llmm = jOperate.queryObject("select * from sc_script where PackagePath=:1 and version=:2",ScriptEntity.class,"org.hocate.test",2.0);
		logger.info(llmm);
	}
}

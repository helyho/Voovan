package org.hocate.test.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hocate.biz.config.ConfigLoader;
import org.hocate.db.JdbcOperate;
import org.hocate.log.Logger;
import org.hocate.script.ScriptEntity;

public class JdbcOperateTest {
	public static void main(String[] args) throws Exception {
		ConfigLoader cl = new ConfigLoader();
		JdbcOperate jOperate = new JdbcOperate(cl.getDataSource());
		
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
		jOperate = new JdbcOperate(cl.getDataSource(),true);
		Logger.info(jOperate.update("update sc_script set version=0"));
		Logger.info(jOperate.queryMapList("select * from sc_script"));
		jOperate.rollback();
	}
}

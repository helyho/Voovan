package org.voovan.test.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.voovan.db.CallType;
import org.voovan.db.JdbcOperate;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JdbcOperateDemo {
	public static void main(String[] args) throws Exception {
		DruidDataSource dataSource = null;
		try {
			String druidpath = TEnv.getSystemPath("Config" + File.separator + "datasource.properties");
			Properties druidProperites = TProperties.getProperties(new File(druidpath));
		    dataSource = TObject.cast(DruidDataSourceFactory.createDataSource(druidProperites));
			dataSource.init();
			Logger.info("Database connection pool init finished");
		} catch (Exception e) {
			Logger.error(e);
		}
		
		JdbcOperate jOperate = new JdbcOperate(dataSource);

		//查询测试
		List<Map<String,Object>> smm = jOperate.queryMapList("select * from sc_script");
		Logger.info(smm);

		//查询并返回多个对象
		List<ScriptEntity> ls = jOperate.queryObjectList("select * from sc_script",ScriptEntity.class);
		Logger.info(ls);

		//Java基本类型
		long count = jOperate.queryObject("select count(*) from sc_script",long.class);
		Logger.info(count);

		//Java基本类型
		String packagePath = jOperate.queryObject("select packagePath from sc_script limit 0,1",String.class);
		Logger.info(packagePath);
		
		//Map参数 => 返回List<Map>
		HashMap<String, Object> xMap = new HashMap<String, Object>();
		xMap.put("packagePath", "org.hocate.test");
		List<Map<String,Object>> mm = jOperate.queryMapList("select * from sc_script where PackagePath=::packagePath",xMap);
		Logger.info(mm);

		//Map参数 => 返回List<Object>
		List<ScriptEntity> lo = jOperate.queryObjectList("select * from sc_script where PackagePath=::packagePath",ScriptEntity.class,xMap);
		Logger.info(lo);
		
		//对象参数 => 返回对象列表 List<Object>
		ScriptEntity sEntity = new ScriptEntity();
		sEntity.setPackagePath("org.hocate.test");
		List<ScriptEntity> lmm = jOperate.queryObjectList("select * from sc_script where PackagePath=::packagePath",ScriptEntity.class,sEntity);
		Logger.info(lmm);

		//对象参数 => 返回对象列表 List<Map>
		List<Map<String,Object>> lmms = jOperate.queryMapList("select * from sc_script where PackagePath=::packagePath",sEntity);
		Logger.info(lmms);
		
		//不定个数参数 => 返回一个复杂对象
		ScriptEntity llmm = jOperate.queryObject("select * from sc_script where PackagePath=::1 and version=::2",ScriptEntity.class,"org.hocate.test",2.0);
		Logger.info(llmm);


		//不定个数参数 => 返回一个复杂对象
		//自动移除不含参数的 SQL 查询条件
		ScriptEntity llmmn = jOperate.queryObject("select * from sc_script where PackagePath=::1 and version=::2",ScriptEntity.class);
		Logger.info(llmmn);

		//事务测试
		jOperate = new JdbcOperate(dataSource,true);
		Logger.info("更新记录数:"+jOperate.update("update sc_script set version=0"));
		Logger.simple("事务回滚前:"+jOperate.queryMapList("select version from sc_script"));
		jOperate.rollback();
		Logger.simple("失误回滚后:"+jOperate.queryMapList("select version from sc_script"));

		//存储过程测试
		jOperate = new JdbcOperate(dataSource);
		Map<String,Object> mmm = new HashMap<String,Object>();
		mmm.put("arg1", "tttt");
		Logger.info(jOperate.call("{call test(::arg1)}",new CallType[]{CallType.INOUT},mmm));
		Logger.info(jOperate.call("{call test(::1)}",new CallType[]{CallType.INOUT},"1111"));
	}
}

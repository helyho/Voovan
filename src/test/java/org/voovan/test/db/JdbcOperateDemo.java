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
        List<Map<String, Object>> smm = jOperate.queryMapList("select * from sc_script");
        Logger.simple("查询测试: "+smm);

        //查询并返回多个对象
        List<ScriptEntity> ls = jOperate.queryObjectList("select * from sc_script", ScriptEntity.class);
        Logger.simple("查询并返回多个对象: "+ls);

        //Java基本类型
        long count = jOperate.queryObject("select count(*) from sc_script", long.class);
        Logger.simple("Java基本类型: "+count);

        //Java基本类型
        String packagePath = jOperate.queryObject("select packagePath from sc_script limit 0,1", String.class);
        Logger.simple("Java基本类型: "+packagePath);

        //Map参数 => 返回List<Map>
        HashMap<String, Object> xMap = new HashMap<String, Object>();
        xMap.put("packagePath", "org.hocate.test");
        List<Map<String, Object>> mm = jOperate.queryMapList("select * from sc_script where PackagePath=::packagePath and version=::version", xMap);
        Logger.simple("Map参数 => 返回List<Map>: "+mm);

        //Map参数 => 返回List<Object>
        List<ScriptEntity> lo = jOperate.queryObjectList("select * from sc_script where PackagePath=::packagePath", ScriptEntity.class, xMap);
        Logger.simple("Map参数 => 返回List<Object>: "+lo);

        //对象参数 => 返回对象列表 List<Object>
        ScriptEntity sEntity = new ScriptEntity();
        sEntity.setPackagePath("org.hocate.test");
        List<ScriptEntity> lmm = jOperate.queryObjectList("select * from sc_script where PackagePath=::packagePath", ScriptEntity.class, sEntity);
        Logger.simple("对象参数 => 返回对象列表 List<Object>: "+lmm);

        //对象参数 => 返回对象列表 List<Map>
        List<Map<String, Object>> lmms = jOperate.queryMapList("select * from sc_script where PackagePath=::packagePath", sEntity);
        Logger.simple("查询并返回多个对象: "+lmms);

        //不定个数参数 => 返回一个复杂对象
        ScriptEntity llmm = jOperate.queryObject("select * from sc_script where PackagePath=::1 and version=::2", ScriptEntity.class, "org.hocate.test", 2.0);
        Logger.simple("不定个数参数 => 返回一个复杂对象: "+llmm);


        //不定个数参数 => 返回一个复杂对象
        List<ScriptEntity> llmmn = jOperate.queryObjectList("select * from sc_script where PackagePath=::1 and version=::2", ScriptEntity.class);
        Logger.simple("自动移除无对应参数的 SQL 查询条件: "+llmmn);

        //数据库中表的列名和对象中的属性名模糊匹配
        //packagePath 列名转换为 paCKAge_Path
        //SouRCEPath 列名转换为 Source_Path
        List<ScriptEntity> llmmnx = jOperate.queryObjectList("select ID,packagePath as paCKAge_Path,SouRCEPath as Source_Path from sc_script", ScriptEntity.class);
        Logger.simple("数据库中表的列名和对象中的属性名模糊匹配: "+llmmnx);

        //事务测试
        jOperate = new JdbcOperate(dataSource, true);
        Logger.simple("事务更新记录数:" + jOperate.update("update sc_script set version=0"));
        Logger.simple("事务回滚前:" + jOperate.queryMapList("select version from sc_script"));
        jOperate.rollback();
        Logger.simple("事务误回滚后:" + jOperate.queryMapList("select version from sc_script"));

        //存储过程测试
        jOperate = new JdbcOperate(dataSource);
        Map<String, Object> mmm = new HashMap<String, Object>();
        mmm.put("arg1", "tttt");
        List<Object> proRes = jOperate.call("{call test(::arg1)}", new CallType[]{CallType.INOUT}, mmm);
        Logger.simple("存储过程测试: "+proRes);
        proRes = jOperate.call("{call test(::1)}", new CallType[]{CallType.INOUT}, "1111");
        Logger.simple("存储过程测试: "+ proRes);
    }
}

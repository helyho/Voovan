package org.voovan.test.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.voovan.db.CallType;
import org.voovan.db.JdbcOperate;
import org.voovan.tools.TFile;
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
            String druidpath = TFile.getSystemPath("classes" + File.separator + "database.properties");
            Properties druidProperites = TProperties.getProperties(new File(druidpath));
            dataSource = (DruidDataSource)DruidDataSourceFactory.createDataSource(druidProperites);
            dataSource.init();
            Logger.info("Database connection pool init finished");
        } catch (Exception e) {
            Logger.error(e);
        }

        JdbcOperate jOperate = new JdbcOperate(dataSource);
        String sql = "";
        //查询测试
        sql = "select * from sc_script";
        List<Map<String, Object>> manyMaps = jOperate.queryMapList(sql);
        Logger.simple("查询测试: " + manyMaps);

        //查询并返回多个对象
        sql = "select * from sc_script";
        List<ScriptEntity> manyObject = jOperate.queryObjectList(sql, ScriptEntity.class);
        Logger.simple("查询并返回多个对象: " + manyObject);

        //Java基本类型(int)
        sql = "select count(*) from sc_script";
        long count = jOperate.queryObject(sql, long.class);
        Logger.simple("Java基本类型: " + count);

        //Java基本类型(String)
        sql = "select packagePath from sc_script";
        String singleField = jOperate.queryObject(sql, String.class);
        Logger.simple("Java基本类型: " + singleField);

        //Map参数 => 返回List<Map>
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("packagePath", "org.hocate.test");
        sql = "select * from sc_script where PackagePath=::packagePath and version=::version";
        List<Map<String, Object>> manyMapsMapParam = jOperate.queryMapList(sql, paramMap);
        Logger.simple("Map参数 => 返回List<Map>: " + manyMapsMapParam);

        //Map参数 => 返回List<Object>
        sql = "select * from sc_script where PackagePath=::packagePath";
        List<ScriptEntity> manyObjectMapParam = jOperate.queryObjectList(sql, ScriptEntity.class, paramMap);
        Logger.simple("Map参数 => 返回List<Object>: " + manyObjectMapParam);

        //对象参数 => 返回对象列表 List<Object>
        ScriptEntity sEntity = new ScriptEntity();
        sEntity.setPackagePath("org.hocate.test");
        sql = "select * from sc_script where PackagePath=::packagePath";
        List<ScriptEntity> manyObjectObjectParam = jOperate.queryObjectList(sql, ScriptEntity.class, sEntity);
        Logger.simple("对象参数 => 返回对象列表 List<Object>: " + manyObjectObjectParam);

        //对象参数 => 返回对象列表 List<Map>
        sql = "select * from sc_script where PackagePath=::packagePath";
        List<Map<String, Object>> manyMapsObjectParam = jOperate.queryMapList(sql, sEntity);
        Logger.simple("查询并返回多个对象: " + manyMapsObjectParam);

        //不定个数参数 => 返回一个复杂对象
        sql = "select * from sc_script where PackagePath=::1 and version=::2";
        ScriptEntity singleObjectArrayParam = jOperate.queryObject(sql, ScriptEntity.class, "org.hocate.test", 2.0);
        Logger.simple("不定个数参数 => 返回一个复杂对象: " + singleObjectArrayParam);


        //不定个数参数 => 返回一个复杂对象
        sql = "select * from sc_script where PackagePath=::1 and version=::2";
        List<ScriptEntity> manyObjectArrayParam = jOperate.queryObjectList(sql, ScriptEntity.class);
        Logger.simple("自动移除无对应参数的 SQL 查询条件: " + manyObjectArrayParam);

        //数据库中表的列名和对象中的属性名模糊匹配
        //packagePath 列名转换为 paCKAge_Path
        //SouRCEPath 列名转换为 Source_Path
        sql = "select ID,packagePath as paCKAge_Path,SouRCEPath as Source_Path from sc_script";
        List<ScriptEntity> manyObjectIgnoreCaseField = jOperate.queryObjectList(sql, ScriptEntity.class);
        Logger.simple("数据库中表的列名和对象中的属性名模糊匹配: " + manyObjectIgnoreCaseField);

        //事务测试
        jOperate = new JdbcOperate(dataSource, true);
        sql = "update sc_script set version=::1";
        int updateCount = jOperate.update(sql,-1);
        Logger.simple("事务1更新记录数:" + updateCount);
        List<Map<String, Object>> updateResult = jOperate.queryMapList("select version from sc_script");
        Logger.simple("事务1更新后:" + updateResult);

        JdbcOperate jOperate_sub = new JdbcOperate(dataSource, true);
        updateCount = jOperate_sub.update(sql,-2);
        Logger.simple("事务2更新记录数:" + updateCount);
        updateResult = jOperate_sub.queryMapList("select version from sc_script");
        Logger.simple("事务2回滚后:" + updateResult);

        jOperate_sub.rollback();
        List<Map<String, Object>> rollbackResult = jOperate.queryMapList("select version from sc_script");
        Logger.simple("事务误回滚后:" + rollbackResult);

        jOperate.commit(true);

        //存储过程测试
        jOperate = new JdbcOperate(dataSource);
        Map<String, Object> procParam = new HashMap<String, Object>();
        procParam.put("arg1", "tttt");
        sql = "{call test(::arg1)}";
        List<Object> callWithMap = jOperate.call(sql, new CallType[]{CallType.INOUT}, procParam);
        Logger.simple("存储过程测试: " + callWithMap);
        sql = "{call test(::1)}";
        List<Object> callWithParam = jOperate.call(sql, new CallType[]{CallType.INOUT}, "1111");
        Logger.simple("存储过程测试: " + callWithParam);
    }
}

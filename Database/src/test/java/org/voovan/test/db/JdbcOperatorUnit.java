package org.voovan.test.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.voovan.db.CallType;
import org.voovan.db.JdbcOperate;
import org.voovan.tools.TFile;
import org.voovan.tools.TProperties;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import junit.framework.TestCase;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 类文字命名
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JdbcOperatorUnit extends TestCase {
    private static DruidDataSource dataSource = null;
    private String sql = "";


    public JdbcOperatorUnit() {
        //只构建一次数据源
        if(dataSource==null) {
            try {
                String druidpath = TFile.getSystemPath("/classes/database.properties");
                Properties druidProperites = TProperties.getProperties(new File(druidpath));
                dataSource = (DruidDataSource)DruidDataSourceFactory.createDataSource(druidProperites);
                dataSource.init();
                Logger.info("Database connection pool init finished");
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    /**
     * 测试返回 List<Map>
     * @throws Exception
     */
    public void test_NoParam_ManyMapResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        //查询测试
        sql = "select * from sc_script";
        List<Map<String, Object>> manyMaps = jOperate.queryMapList(sql);
        Logger.simple("查询测试: " + manyMaps);
        assert(manyMaps.size()==2);
    }

    /**
     * 测试返回 List<Object>
     * @throws Exception
     */
    public void test_NoParam_ManyObjectResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        sql = "select * from sc_script";
        List<ScriptEntity> manyObject = jOperate.queryObjectList(sql, ScriptEntity.class);
        Logger.simple("查询并返回多个对象: " + manyObject);
        assert(manyObject.size()==2);
        assert(manyObject.get(0).getClass().equals(ScriptEntity.class));
    }

    /**
     * 非对象类型测试
     * @throws Exception
     */
    public void test_NoParam_SimpleTypeResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        sql = "select count(*) from sc_script";
        long count = jOperate.queryObject(sql, long.class);
        Logger.simple("Java基本类型: " + count);
        assert(count == 2);
    }

    /**
     * 简单对象类型测试
     * @throws Exception
     */
    public void test_NoParam_ObjectResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        sql = "select packagePath from sc_script";
        String singleField = jOperate.queryObject(sql, String.class);
        Logger.simple("Java基本类型: " + singleField);
        assert(singleField.equals("org.hocate.test"));
    }

    /**
     * Map参数 => 返回List<Map>
     * @throws Exception
     */
    public void test_MapParam_ManyMapResult_RemoveCondiction() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("packagePath", "org.hocate.test");
        paramMap.put("version","1.0");
        sql = "select * from sc_script where PackagePath=::packagePath and version=::version";
        List<Map<String, Object>> manyMapsMapParam = jOperate.queryMapList(sql, paramMap);
        Logger.simple("Map参数 => List<Map>: " + manyMapsMapParam);
        assert(manyMapsMapParam.size()==1);
        assertEquals(manyMapsMapParam.get(0).get("packagePath"),"org.hocate.test");
    }

    /**
     * Map参数 => 返回List<Object>
     * @throws Exception
     */
    public void test_MapParam_ManyObjectResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("packagePath", "org.hocate.test");

        sql = "select * from sc_script where PackagePath=::packagePath";
        List<ScriptEntity> manyObjectMapParam = jOperate.queryObjectList(sql, ScriptEntity.class, paramMap);
        Logger.simple("Map参数 => List<Object>: " + manyObjectMapParam);
        assert(manyObjectMapParam.size()==2);
        assert(manyObjectMapParam.get(0).getClass().equals(ScriptEntity.class));
    }

    /**
     * 对象参数 => 返回对象列表 List<Object>
     * @throws Exception
     */
    public void test_ObjectParam_ManyObjectResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        ScriptEntity sEntity = new ScriptEntity();
        sEntity.setPackagePath("org.hocate.test");

        sql = "select * from sc_script where PackagePath=::packagePath";
        List<ScriptEntity> manyObjectObjectParam = jOperate.queryObjectList(sql, ScriptEntity.class, sEntity);
        Logger.simple("对象参数 => List<Object>: " + manyObjectObjectParam);
        assert(manyObjectObjectParam.size()==2);
        assert(manyObjectObjectParam.get(0).getClass().equals(ScriptEntity.class));
    }

    /**
     * 对象参数 => 返回对象列表 List<Map>
     * @throws Exception
     */
    public void test_ObjectParam_ManyMapResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);
        ScriptEntity sEntity = new ScriptEntity();
        sEntity.setPackagePath("org.hocate.test");

        sql = "select * from sc_script where PackagePath=::packagePath";
        List<Map<String, Object>> manyMapsObjectParam = jOperate.queryMapList(sql, sEntity);
        Logger.simple("对象参数 => List<Map>: " + manyMapsObjectParam);

        assert(manyMapsObjectParam.size()==2);
    }

    /**
     * 不定个数参数 => 返回一个复杂对象:
     * @throws Exception
     */
    public void test_ManyParam_ObjectResult() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);

        sql = "select * from sc_script where PackagePath=::1 and version=::2";
        ScriptEntity singleObjectArrayParam = jOperate.queryObject(sql, ScriptEntity.class, "org.hocate.test", 2.0);
        Logger.simple("不定个数参数 => Object: " + singleObjectArrayParam);
        assertEquals(singleObjectArrayParam.getClass(), ScriptEntity.class);
    }

    /**
     * 没有传入 PackagePath 和 version 参数自动移除 SQL 中的 PackagePath 和 version 条件
     * 具体SQL的变化,查看日志实际执行 sql 的输出
     * @throws Exception
     */
    public void test_RemoveCondiction() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);

        sql = "select * from sc_script where PackagePath=::1 and version=::2";
        List<ScriptEntity> manyObjectArrayParam = jOperate.queryObjectList(sql, ScriptEntity.class);
        Logger.simple("自动移除无对应参数的 SQL 查询条件: " + manyObjectArrayParam);
        assertEquals(manyObjectArrayParam.size(),2);
    }

    /**
     * 数据库中表的列名和对象中的属性名模糊匹配
     * 不区分大小写
     * packagePath 列名转换为 paCKAge_Path,SouRCEPath 列名转换为 Source_Path
     * 自动对应到ScriptEntity对象的属性上
     * @throws Exception
     */
    public void test_FillObjectIgnoreCase() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);

        sql = "select ID,packagePath as paCKAge_Path,SouRCEPath as Source_Path from sc_script";
        List<ScriptEntity> manyObjectIgnoreCaseField = jOperate.queryObjectList(sql, ScriptEntity.class);
        Logger.simple("数据库中表的列名和对象中的属性名模糊匹配: " + manyObjectIgnoreCaseField);
        assertEquals(manyObjectIgnoreCaseField.size(),2);
    }

    /**
     * 更新和事物测试
     * @throws Exception
     */
    public void test_Update_Trancation() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);

        jOperate = new JdbcOperate(dataSource, true);
        sql = "update sc_script set version=::1";
        int updateCount = jOperate.update(sql,-1);
        assert(updateCount==2);
        Logger.simple("事务更新记录数:" + updateCount);
        List<Map<String, Object>> updateResult = jOperate.queryMapList("select version from sc_script");
        assertEquals(updateResult.get(0).get("version"),Float.valueOf(-1));
        Logger.simple("事务回滚前:" + updateResult);
        jOperate.rollback();
        List<Map<String, Object>> rollbackResult = jOperate.queryMapList("select version from sc_script");
        Logger.simple("事务误回滚后:" + rollbackResult);
        assert(!rollbackResult.get(0).get("version").equals(Float.valueOf(-1)));
    }


    /**
     * 更新和事物测试
     * @throws Exception
     */
    public void test_Update_Mutil_Trancation() throws Exception {
        //使用下面的 sql 做数据准备
//        DROP TABLE sc_script;
//        CREATE TABLE `sc_script` (
//          `id` int(11) NOT NULL,
//          `version` varchar(255) COLLATE utf8_bin DEFAULT NULL,
//                PRIMARY KEY (`id`)
//        ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
//        INSERT INTO `Ticker`.`sc_script`(`id`, `version`) VALUES (1, 'ver_1');
//        INSERT INTO `Ticker`.`sc_script`(`id`, `version`) VALUES (2, 'ver_2');
//        INSERT INTO `Ticker`.`sc_script`(`id`, `version`) VALUES (3, 'ver_3');
//        更新三条数据后全部回滚
//        观察最后的结果数据没有任何变化 依旧是 ver_x 的形式

        JdbcOperate jOperate = new JdbcOperate(dataSource);

        jOperate = new JdbcOperate(dataSource, true);
        sql = "update sc_script set version=::1 where id = ::2";
        int updateCount = jOperate.update(sql, "ver_1_1", 1);
        updateCount += jOperate.update(sql, "ver_2_2", 2);
        updateCount += jOperate.update(sql, "ver_2_2", 3);

        Logger.simple("事务更新记录数:" + updateCount);
        List<Map<String, Object>> updateResult = jOperate.queryMapList("select version from sc_script");
        Logger.simple("事务回滚前:" + JSON.toJSON(updateResult));
        jOperate.rollback();
        List<Map<String, Object>> rollbackResult = jOperate.queryMapList("select version from sc_script");
        Logger.simple("事务误回滚后:" + JSON.toJSON(rollbackResult));
    }

    /**
     * 更新和事物测试
     * @throws Exception
     */
    public void test_Update_Mutil_Part_Trancation() throws Exception {
        //使用下面的 sql 做数据准备
//        DROP TABLE sc_script;
//        CREATE TABLE `sc_script` (
//          `id` int(11) NOT NULL,
//          `version` varchar(255) COLLATE utf8_bin DEFAULT NULL,
//                PRIMARY KEY (`id`)
//        ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
//        INSERT INTO `Ticker`.`sc_script`(`id`, `version`) VALUES (1, 'ver_1');
//        INSERT INTO `Ticker`.`sc_script`(`id`, `version`) VALUES (2, 'ver_2');
//        INSERT INTO `Ticker`.`sc_script`(`id`, `version`) VALUES (3, 'ver_3');
//        更新第一条数据后提交,接着更新两条数据后回滚
//        观察最后的结果数据第一条变成 ver_1_1, 后面两条依旧是 ver_x 的形式

        JdbcOperate jOperate = new JdbcOperate(dataSource);

        jOperate = new JdbcOperate(dataSource, true);
        sql = "update sc_script set version=::1 where id = ::2";
        int updateCount = jOperate.update(sql, "ver_1_1", 1);
        jOperate.commit(false);
        Logger.simple("只提交第一条:" + updateCount);

        updateCount += jOperate.update(sql, "ver_2_2", 2);
        updateCount += jOperate.update(sql, "ver_2_2", 3);

        Logger.simple("事务更新记录数:" + updateCount);
        List<Map<String, Object>> updateResult = jOperate.queryMapList("select version from sc_script");
        Logger.simple("事务回滚前:" + JSON.toJSON(updateResult));
        jOperate.rollback();
        List<Map<String, Object>> rollbackResult = jOperate.queryMapList("select version from sc_script");
        Logger.simple("事务误回滚后:" + JSON.toJSON(rollbackResult));
        Logger.simple("");
    }

    public void test_Procedure() throws Exception {
        JdbcOperate jOperate = new JdbcOperate(dataSource);

        jOperate = new JdbcOperate(dataSource);
        Map<String, Object> procParam = new HashMap<String, Object>();
        procParam.put("arg1", "tttt");
        sql = "{call test(::arg1)}";
        List<Object> callWithMap = jOperate.call(sql, new CallType[]{CallType.INOUT}, procParam);
        assert(callWithMap.get(0).equals("org.hocate.test"));
        Logger.simple("存储过程测试: " + callWithMap);
        sql = "{call test(::1)}";
        List<Object> callWithParam = jOperate.call(sql, new CallType[]{CallType.INOUT}, "1111");
        Logger.simple("存储过程测试: " + callWithParam);
        assert(callWithParam.get(0).equals("org.hocate.test"));
    }

}

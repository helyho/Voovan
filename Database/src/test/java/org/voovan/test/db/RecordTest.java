package org.voovan.test.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.voovan.db.JdbcOperate;
import org.voovan.db.recorder.Query;
import org.voovan.db.recorder.Recorder;
import org.voovan.db.recorder.annotation.Table;
import org.voovan.db.recorder.exception.RecorderException;
import junit.framework.TestCase;
import org.voovan.tools.TFile;
import org.voovan.tools.TProperties;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.util.Properties;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Table(lowerCase = 1)
public class RecordTest extends TestCase{

    private static DruidDataSource dataSource = null;
    private String sql = "";


    public RecordTest() {
        //只构建一次数据源
        if(dataSource==null) {
            try {
                String druidpath = TFile.getSystemPath("/classes/database.properties");
                Properties druidProperites = TProperties.getProperties(new File(druidpath));
                dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(druidProperites);
                dataSource.init();
                Logger.info("Database connection pool init finished");
            } catch (Exception e) {
                Logger.error(e);
            }
        }
    }

    /**
     * 测试方法
     * @throws RecorderException
     */
    public void test_Query() throws RecorderException {

        ScriptEntity scriptEntity = new ScriptEntity();
        scriptEntity.setPackagePath("org.hocate.test");

        //构造查询条件
        Query query = Query.newInstance().addResult("version").addAnd("packagePath").addOrder(true, "id").page(2,10);

        //查询测试
//        System.out.println(new Recorder(new JdbcOperate(dataSource), false).query("sc_script", scriptEntity, query));
        System.out.println(new Recorder(new JdbcOperate(dataSource), false).query(scriptEntity, query));

    }

    /**
     * 测试方法
     * @throws RecorderException
     */
    public void test_Update() throws RecorderException {

        ScriptEntity scriptEntity = new ScriptEntity();
        scriptEntity.setId(1);
        scriptEntity.setPackagePath("org.hocate.test.main");

        //更新测试
        System.out.println(new Recorder(new JdbcOperate(dataSource), false).update(scriptEntity, Query.newInstance().addResult("packagePath").addAnd("id")));
    }

    /**
     * 测试方法
     * @throws RecorderException
     */
    public void test_Insert() throws RecorderException {

        ScriptEntity scriptEntity = new ScriptEntity();
        scriptEntity.setPackagePath("org.hocate.test.main");
        scriptEntity.setSourcePath("/Users/helyho/Work/BlockLink");

        for(int i=0;i<100;i++) {
            //更新测试
            scriptEntity.setVersion(i);
            System.out.println(new Recorder(new JdbcOperate(dataSource), false).insert(scriptEntity));
        }
    }

    /**
     * 测试方法
     * @throws RecorderException
     */
    public void test_Delete() throws RecorderException {

        ScriptEntity scriptEntity = new ScriptEntity();
        scriptEntity.setId(100);

        //更新测试
        System.out.println(new Recorder(new JdbcOperate(dataSource), false).delete(scriptEntity, Query.newInstance().addAnd("id")));
    }
}



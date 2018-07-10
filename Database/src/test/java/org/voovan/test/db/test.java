package org.voovan.test.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.voovan.db.JdbcOperate;
import org.voovan.tools.TProperties;
import org.voovan.tools.UniqueId;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Semaphore;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class test {

    public static void main(String[] args) throws IOException, InterruptedException {
        UniqueId uniqueId = new UniqueId(10);


        DruidDataSource dataSource = null;


        //只构建一次数据源
        if(dataSource==null) {
            try {
                Properties druidProperites = TProperties.getProperties("database");
                dataSource = (DruidDataSource)DruidDataSourceFactory.createDataSource(druidProperites);
                dataSource.init();
                Logger.info("Database connection pool init finished");
            } catch (Exception e) {
                Logger.error(e);
            }
        }

        Semaphore semaphore = new Semaphore(100);

        while (true){
            try {
                semaphore.acquire();
                String sql = "INSERT INTO `zsdkj_test`.`fund_change1`(`change_id`, `web_id`, `user_id`, `transaction_id`, `fund_id`, `currency_type_id`, `change_amount`, `fee_amount`, `fee_rate`, `original_fee_rate`, `change_type`, `seller_entrust_id`, `buyer_entrust_id`, `market_id`, `create_time`, `remark`, `operator_id`, `snapshot_amount`, `snapshot_freeze`, `convert_amount`, `assist_price`) " +
                        "VALUES ('N_" + uniqueId.nextInt() + "', '100', 'robot_" + Double.valueOf(Math.random()).intValue() * 500 + "', 'F6409274786379202560', 'FT6419584441819484160', 3, 8.028945829193102, 0.004016481155174138, 5.0E-4, 5.0E-4, 2, 'E6419544578671923200', 'E6419529255600148480', '92', '2018-07-02 21:38:16.0', '', '', 9.999999917825034E11, 0.0, -1.0, -1.0);";

                JdbcOperate jdbcOperate = new JdbcOperate(dataSource);
                jdbcOperate.update(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
    }
}

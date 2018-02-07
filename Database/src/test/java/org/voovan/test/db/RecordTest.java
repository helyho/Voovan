package org.voovan.test.db;

import org.voovan.db.recorder.Query;
import org.voovan.db.recorder.Recorder;
import org.voovan.db.recorder.annotation.Field;
import org.voovan.db.recorder.annotation.PrimaryKey;
import org.voovan.db.recorder.annotation.Table;
import org.voovan.db.recorder.exception.RecorderException;
import junit.framework.TestCase;

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

    //测试类对象
    public class Data {
        @PrimaryKey
        @Field(upperCase = 1)
        public int id = 12312312;
        public String firstName = "github";
        public int age = 123;

        public String getName() {
            return firstName;
        }

        public void setName(String firstName) {
            this.firstName = firstName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    /**
     * 测试方法
     * @throws RecorderException
     */
    public void test_All() throws RecorderException {
        Recorder record = new Recorder(null);

        //更新测试
        System.out.println(record.buildUpdateSqlTemplate(new Data()));

        //插入测试
        System.out.println(record.buildInsertSqlTemplate(new Data()));

        //删除测试
        System.out.println(record.buildDeleteSqlTemplate(new Data()));

        //构造查询条件
        Query query = Query.newInstance().addResult("id").addResult("firstName").addAnd("firstName").AddOr("age", Query.Operate.LESS).addOrder(true, "id").addOrder(false, "age");

        //查询测试
        System.out.println(record.buildQuerySqlTemplate(new Data(), query));
    }
}



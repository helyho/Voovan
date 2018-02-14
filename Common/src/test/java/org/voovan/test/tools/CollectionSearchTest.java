package org.voovan.test.tools;

import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import junit.framework.TestCase;
import org.voovan.tools.CollectionSearch;
import org.voovan.tools.json.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class CollectionSearchTest extends TestCase{
    static class Test {
        public String aaa;
        public int bbb;
        public Map map;

        public Test(int bbb, String aaa){
            this.aaa = aaa;
            this.bbb = bbb;
            this.map = TObject.asMap("123","adfadf");
        }
    }

    public void testOne() {
        List<Test> tl = new ArrayList<Test>();
        for(int i=0;i<30;i++){
            if(i%2==0) {
                tl.add(new Test(i, "fff"+i));
            } else {
                tl.add(new Test(i, "ddd"));
            }
        }

        System.out.println(JSON.toJSON(tl));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", "ddd").sort("bbb", false).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", "ddd").sort("bbb", false).page(2,2).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).sort("bbb").limit(4).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", "ddd").sort("bbb", false).page(2, 4).sort("bbb", false).limit(2).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("bbb", CollectionSearch.Operate.GREATER, 20).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", CollectionSearch.Operate.START_WITH, "fff2").search()));

        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition(new Predicate<Test>() {
            @Override
            public boolean test(Test test) {
                return false;
            }
        }).search()));


    }

    public void testFieldFilter() throws ReflectiveOperationException {
        Test test = new Test(12, "123g");
        Test test1 = new Test(123, "123g");
        Object obj = TReflect.fieldFilter(test, "bbb", "map[12]");
        System.out.println(JSON.toJSON(obj));

        obj = CollectionSearch.newInstance(TObject.asList(test, test1)).fields("bbb", "map[12]");
        System.out.println(JSON.toJSON(obj));
    }
}

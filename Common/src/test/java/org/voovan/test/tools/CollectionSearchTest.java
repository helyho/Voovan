package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.CollectionSearch;
import org.voovan.tools.json.JSON;

import java.util.ArrayList;
import java.util.List;
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
    static class test {
        public String aaa;
        public int bbb;

        public test(int bbb, String aaa){
            this.aaa = aaa;
            this.bbb = bbb;
        }
    }

    public void testOne() {
        List<test> tl = new ArrayList<test>();
        for(int i=0;i<30;i++){
            if(i%2==0) {
                tl.add(new test(i, "fff"+i));
            } else {
                tl.add(new test(i, "ddd"));
            }
        }

        System.out.println(JSON.toJSON(tl));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", "ddd").sort("bbb", false).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", "ddd").sort("bbb", false).page(2,2).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).sort("bbb").limit(4).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", "ddd").sort("bbb", false).page(2, 4).sort("bbb", false).limit(2).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("bbb", CollectionSearch.Operate.GREATER, 20).search()));
        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition("aaa", CollectionSearch.Operate.START_WITH, "fff2").search()));

        System.out.println(JSON.toJSON(CollectionSearch.newInstance(tl).addCondition(new Predicate<test>() {
            @Override
            public boolean test(test test) {
                return false;
            }
        }).search()));


    }
}

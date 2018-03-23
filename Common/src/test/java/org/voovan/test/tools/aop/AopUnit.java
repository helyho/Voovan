package org.voovan.test.tools.aop;

import org.voovan.tools.aop.Aop;
import junit.framework.TestCase;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class AopUnit extends TestCase{
    public void setUp() throws Exception {
        Aop.init("org.voovan.test");
    }

    public void testMethod(){
        CutPointUtil cutPointUtil = new CutPointUtil();
        cutPointUtil.testMethod("aaa");

        System.out.println("==============================================");
        cutPointUtil.testMethod(123);
    }
}

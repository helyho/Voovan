package org.voovan.test.tools.aop;

import junit.framework.TestCase;
import org.voovan.tools.aop.Aop;
import org.voovan.tools.log.Logger;

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

    public void testMethod() throws Exception {
        CutPointUtil cutPointUtil = new CutPointUtil();

        System.out.println("======================Before========================");
        cutPointUtil.testBefore(123);

        System.out.println("======================After========================");
        cutPointUtil.testAfter("aaa");

        System.out.println("======================Around========================");
        Logger.simple(cutPointUtil.testAround(123));

        System.out.println("======================Exception========================");
        cutPointUtil.testException(123);

    }
}

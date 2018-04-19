package org.voovan.test.tools.aop;

import junit.framework.TestCase;
import org.voovan.tools.aop.InterceptInfo;
import org.voovan.tools.aop.annotation.After;
import org.voovan.tools.aop.annotation.Aop;
import org.voovan.tools.aop.annotation.Around;
import org.voovan.tools.aop.annotation.Before;
import org.voovan.tools.json.JSON;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
@Aop
public class CutPointUtil extends TestCase{


    public String testBefore(int mm){
        System.out.println("testMethod2--->" + mm);
        return "ffff";
    }


    public int testAfter(String mm){
        System.out.println("testMethod--->" + mm);
        return 234;
    }

    public int testException(int i) throws java.lang.Exception {
        try {
            throw new ReflectiveOperationException("test exception");
        } catch (java.lang.Exception e){
            throw e;
        }
    }

    public String testAround(int mm){
        System.out.println("around--->" + mm);
        return "ffff" + mm;
    }

    @Before("* org.voovan.test.tools.aop.CutPointUtil@testBefore(..)")
    public static void cutPointBefore(InterceptInfo interceptInfo){
        System.out.println("before========>");
    }

    @After("* org.voovan.test.*.*.CutPointUtil@testAfter(java.lang.String)")
    public static String cutPointAfter(InterceptInfo interceptInfo){
        System.out.println("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }

    @Around("* org.voovan.test.*.*.CutPointUtil@testAround(int)")
    public static String cutPointAround(InterceptInfo interceptInfo) throws Throwable {
        System.out.println("after========>" + JSON.toJSON(interceptInfo));
        Object result = interceptInfo.process();
        return "-----////" + result;
    }

    @org.voovan.tools.aop.annotation.Exception("* org.voovan.test.*.*.CutPointUtil@testException(int)")
    public static String cutPointCatch(InterceptInfo interceptInfo){
        System.out.println("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }

    @After("* org.voovan.test.http.router.AnnotationRouterTest@index()")
    public static String indexMethodAfter(InterceptInfo interceptInfo){
        System.out.println("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }
}

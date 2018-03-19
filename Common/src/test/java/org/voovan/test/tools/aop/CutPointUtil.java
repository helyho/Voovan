package org.voovan.test.tools.aop;

import org.voovan.tools.aop.InterceptInfo;
import org.voovan.tools.aop.annotation.After;
import org.voovan.tools.aop.annotation.Aop;
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
public class CutPointUtil {

    public String testMethod(String mm){
        System.out.println("testMethod--->" + mm);
        return "ffff";
    }

    public String testMethod(int mm){
        System.out.println("testMethod2--->" + mm);
        return "ffff";
    }

    @Before("* org.voovan.test.tools.aop.CutPointUtil@testMethod(..)")
    public static void cutPointBefore(InterceptInfo interceptInfo){
        System.out.println("before========>");
    }

    @After("* org.voovan.test.*.*.CutPointUtil@testMethod(java.lang.String)")
    public static String cutPointAfter(InterceptInfo interceptInfo){
        System.out.println("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }

    @After("* org.voovan.test.http.router.AnnotationRouterTest@index()")
    public static String indexMethodAfter(InterceptInfo interceptInfo){
        System.out.println("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }
}

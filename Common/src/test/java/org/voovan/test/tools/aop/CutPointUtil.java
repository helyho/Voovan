package org.voovan.test.tools.aop;

import org.voovan.tools.aop.InterceptInfo;
import org.voovan.tools.aop.annotation.*;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

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


    public String testBefore(int mm){
        Logger.simple("testMethod2--->" + mm);
        return "ffff";
    }


    public int testAfter(String mm){
        Logger.simple("testMethod--->" + mm);
        return 234;
    }

    public int testException(int i) throws java.lang.Exception {
        try {
            throw new ReflectiveOperationException("test exception");
        } catch (java.lang.Exception e){
            throw e;
        }
    }

    public static String testAround(int mm){
        Logger.simple("around--->" + mm);
        return "ffff" + mm;
    }

    private void mmm(){

    }

    @Before("* org.voovan.test.tools.aop.CutPointUtil@testBefore(..)")
    public static void cutPointBefore(InterceptInfo interceptInfo){
        Logger.simple("before========>");
    }

    @After("* org.voovan.test.*.*.CutPointUtil@testAfter(java.lang.String)")
    public static String cutPointAfter(InterceptInfo interceptInfo){
        Logger.simple("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }

    @Around("* org.voovan.test.*.*.CutPointUtil@testAround(int)")
    public static String cutPointAround(InterceptInfo interceptInfo) throws Throwable {
        Logger.simple("Around========>" + JSON.toJSON(interceptInfo));
        Object result = interceptInfo.process();
        return "-----////" + result;
    }

    @Abnormal("* org.voovan.test.*.*.CutPointUtil@testException(int)")
    public static String cutPointCatch(InterceptInfo interceptInfo){
        Logger.simple("Abnormal========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }

    @After("void org.voovan.test.*.*.CutPointUtil@mmm()")
    public static String testmmmm(InterceptInfo interceptInfo){
        Logger.simple("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }

    @After("* org.voovan.test.service.web.IndexMethod@index(..)")
    public static String indexMethodAfter(InterceptInfo interceptInfo){
        Logger.simple("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }

    @Around("* org.voovan.test.service.web.IndexMethod@index(..)")
    public static String indexMethodAround(InterceptInfo interceptInfo){
//        Logger.simple("Around========>" + JSON.toJSON(interceptInfo));
        Object object = null;
        try {
            object = interceptInfo.process();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return "-----////" + object;
    }
}

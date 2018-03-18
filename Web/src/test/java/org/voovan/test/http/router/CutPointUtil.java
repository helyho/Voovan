package org.voovan.test.http.router;

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

    @After("* org.voovan.test.http.router.AnnotationRouterTest@index()")
    public static String indexMethodAfter(InterceptInfo interceptInfo){
        System.out.println("after========>" + JSON.toJSON(interceptInfo));
        return "-----////";
    }
}

package org.voovan.http.server.router;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.router.annotation.Router;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 通过注解实现的路由
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationRouter implements HttpRouter {

    /**
     * 扫描包含Router注解的类
     *
     * @throws IOException                  IO 异常
     * @throws ReflectiveOperationException 反射异常
     */
    public void scanRouterClass() throws IOException, ReflectiveOperationException {
        int pageMethodNum = 0;
        //查找
        List<Class> routerClasses = TEnv.searchClassInEnv(null, new Class[]{Router.class});
        for (Class routerClass : routerClasses) {
            Method[] methods = routerClass.getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Router.class)) {

                    Router routeAnnotation = method.getAnnotation(Router.class);
                    if (routeAnnotation!=null) {


                    }
                }
            }
        }

        Logger.simple(TFile.getLineSeparator() + "[SYSTEM] Pages类扫描完毕,共有Pages类" + routerClasses.size() + "个，未注册的 page 注解的函数" + pageMethodNum + "个.");
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {

    }
}

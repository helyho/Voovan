package org.voovan.http.server.module.annontationRouter.router;

import org.voovan.http.server.*;
import org.voovan.http.server.exception.AnnotationRouterException;
import org.voovan.http.server.module.annontationRouter.AnnotationModule;
import org.voovan.http.server.module.annontationRouter.annotation.*;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过注解实现的路由
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AnnotationRouter implements HttpRouter {

    private static Map<Class, Object> singletonObjs = new ConcurrentHashMap<Class, Object>();

    private Class clazz;
    private Method method;
    private Router classRouter;
    private Router methodRoute;

    /**
     * 构造函数
     * @param clazz   Class对象
     * @param method  方法对象
     * @param classRouter 类上的 Route 注解
     * @param methodRoute 方法上的 Route 注解
     */
    public AnnotationRouter(Class clazz, Method method, Router classRouter, Router methodRoute) {
        this.clazz = clazz;
        this.method = method;
        this.classRouter = classRouter;
        this.methodRoute = methodRoute;

        //如果是单例,则进行预实例化
        if(classRouter.singleton() && !singletonObjs.containsKey(clazz)){
            try {
                singletonObjs.put(clazz, clazz.newInstance());
            } catch (Exception e) {
                Logger.error("New a singleton object error", e);
            }
        }
    }

    /**
     * 扫描包含Router注解的类
     *
     * @param httpModule   AnnotationModule对象用于注册路由
     */
    public static void scanRouterClassAndRegister(AnnotationModule httpModule) {
        int routeMethodNum = 0;
        WebServer webServer = httpModule.getWebServer();
        try {
            //查找包含 Router 注解的类
            List<Class> routerClasses = TEnv.searchClassInEnv(httpModule.getScanRouterPackage(), new Class[]{Router.class});
            for (Class routerClass : routerClasses) {
                Method[] methods = routerClass.getMethods();
                Router[] annonClassRouters = (Router[]) routerClass.getAnnotationsByType(Router.class);

                //多个 Router 注解的迭代
                for(Router annonClassRouter : annonClassRouters) {
                    String classRouterPath = annonClassRouter.path().isEmpty() ? annonClassRouter.value() : annonClassRouter.path();
                    String[] classRouterMethods = annonClassRouter.method();

                    //多个请求方法的迭代
                    for(String classRouterMethod : classRouterMethods) {

                        //使用类名指定默认路径
                        if (classRouterPath.isEmpty()) {
                            //使用类名指定默认路径
                            classRouterPath = routerClass.getSimpleName();
                        }

                        classRouterPath = fixAnnotationRoutePath(classRouterPath);

                        //扫描包含 Router 注解的方法
                        for (Method method : methods) {
                            Router[] annonMethodRouters = (Router[]) method.getAnnotationsByType(Router.class);
                            if (annonMethodRouters != null) {

                                //多个 Router 注解的迭代
                                for (Router annonMethodRouter : annonMethodRouters) {
                                    String methodRouterPath = annonMethodRouter.path().isEmpty() ? annonMethodRouter.value() : annonMethodRouter.path();
                                    String[] methodRouterMethods = annonMethodRouter.method();

                                    //多个请求方法的迭代
                                    for (String methodRouterMethod : methodRouterMethods) {

                                        //使用方法名指定默认路径
                                        if (methodRouterPath.isEmpty()) {
                                            //如果方法名为: index 则为默认路由
                                            if (method.getName().equals("index")) {
                                                methodRouterPath = "/";
                                            } else {
                                                methodRouterPath = method.getName();
                                            }
                                        }

                                        //拼装方法路径
                                        methodRouterPath = fixAnnotationRoutePath(methodRouterPath);
                                        methodRouterPath = TString.assembly("/", methodRouterPath);

                                        //拼装 (类+方法) 路径
                                        String routePath = classRouterPath + methodRouterPath;

                                        //如果方法上的注解指定了 Method 则使用方法上的注解指定的,否则使用类上的注解指定的
                                        String routeMethod = methodRouterMethod != null ? methodRouterMethod : classRouterMethod;

                                        //为方法的参数准备带参数的路径
                                        String paramPath = "";
                                        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                                        Class[] parameterTypes = method.getParameterTypes();
                                        for (int i = 0; i < parameterAnnotations.length; i++) {
                                            Annotation[] annotations = parameterAnnotations[i];

                                            if (annotations.length == 0 &&
                                                    parameterTypes[i] != HttpRequest.class &&
                                                    parameterTypes[i] != HttpResponse.class &&
                                                    parameterTypes[i] != HttpSession.class) {
                                                paramPath = paramPath + "/:param" + (i + 1);
                                                continue;
                                            }

                                            for (Annotation annotation : annotations) {
                                                if (annotation instanceof Param) {
                                                    paramPath = TString.assembly(paramPath, "/:", ((Param) annotation).value());
                                                }
                                            }
                                        }

                                        /**
                                         * 注册路由部分代码在下面
                                         */
                                        if (webServer.getHttpRouters().get(routeMethod) != null) {

                                            //生成完整的路由,用来检查路由是否存在
                                            routePath = HttpDispatcher.fixRoutePath(routePath);

                                            //这里这么做是为了处理 TreeMap 的 containsKey 方法的 bug
                                            Map routerMaps = new HashMap();
                                            routerMaps.putAll(webServer.getHttpRouters().get(routeMethod));

                                            //构造注解路由器
                                            AnnotationRouter annotationRouter = new AnnotationRouter(routerClass, method, annonClassRouter, annonMethodRouter);

                                            //1.注册路由, 处理不在参数的路由
                                            {
                                                routePath = "/" + fixAnnotationRoutePath(routePath);
                                                //判断路由是否注册过
                                                if (!routerMaps.containsKey(routePath)) {
                                                    //注册路由,不带路径参数的路由
                                                    httpModule.otherMethod(routeMethod, routePath, annotationRouter);
                                                    Logger.simple("[SYSTEM] Module [" + httpModule.getModuleConfig().getName() +
                                                            "] Router add annotation route: " + TString.rightPad(routeMethod, 8, ' ') +
                                                            httpModule.getModuleConfig().getPath() + routePath);
                                                    routeMethodNum++;
                                                }
                                            }

                                            //2.注册路由,带路径参数的路由
                                            if (!paramPath.isEmpty()) {
                                                String routeParamPath = null;
                                                routeParamPath = routePath + paramPath;
                                                routeParamPath = "/" + fixAnnotationRoutePath(routeParamPath);

                                                if (!routerMaps.containsKey(routeParamPath)) {
                                                    httpModule.otherMethod(routeMethod, routeParamPath, annotationRouter);

                                                    Logger.simple("[SYSTEM] Module [" + httpModule.getModuleConfig().getName() +
                                                            "] Router add annotation route: " + TString.rightPad(routeMethod, 8, ' ') +
                                                            httpModule.getModuleConfig().getPath() + routeParamPath);
                                                    routeMethodNum++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if(routeMethodNum>0) {
                Logger.simple(TFile.getLineSeparator() + "[SYSTEM] Module [" + httpModule.getModuleConfig().getName() +
                        "] Scan some class annotation by Router: " + routerClasses.size() +
                        ". Register Router method annotation by route: " + routeMethodNum + ".");
            }
        } catch (Exception e){
            Logger.error("Scan router class error.", e);
        }
    }

    /**
     * 修复路由路径
     * @param routePath 路由路径
     * @return 修复后的路由路径
     */
    private static String fixAnnotationRoutePath(String routePath){
        routePath = routePath.startsWith("/") ? TString.removePrefix(routePath) : routePath;
        routePath = routePath.endsWith("/") ? TString.removeSuffix(routePath) : routePath;
        return routePath;
    }

    /**
     * 将一个 Http 请求映射到一个类的方法调用
     * @param request   http 请求对象
     * @param response  http 响应对象
     * @param clazz     Class 对象
     * @param method    Method 对象
     * @return  返回值
     * @throws Exception 调用过程中的异常
     */
    public Object invokeRouterMethod(HttpRequest request, HttpResponse response, Class clazz, Method method) throws Exception {

        Object annotationObj = null;

        //如果是单例模式则使用预先初始话好的
        if(this.classRouter.singleton()){
            annotationObj = singletonObjs.get(clazz);
        } else {
            annotationObj = clazz.newInstance();
        }

        Class[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        String bodyString = "";
        Map bodyMap = null;
        if(request.body().size() > 0 && JSON.isJSONMap(bodyString)) {
            bodyString = request.body().getBodyString();
            bodyMap =  (Map) JSON.parse(bodyString);
        }

        //准备参数
        Object[] params = new Object[parameterTypes.length];
        for(int i=0; i < parameterAnnotations.length; i++){

            //请求对象
            if(parameterTypes[i] == HttpRequest.class){
                params[i] = request;
                continue;
            }

            //响应对象
            if(parameterTypes[i] == HttpResponse.class){
                params[i] = response;
                continue;
            }

            //会话对象
            if(parameterTypes[i] == HttpSession.class){
                params[i] = request.getSession();
                continue;
            }

            for(Annotation annotation : parameterAnnotations[i]) {

                //请求的参数
                if (annotation instanceof Param) {
                    String paramName = ((Param) annotation).value();
                    params[i] = TString.toObject(request.getParameter(paramName), parameterTypes[i], true);
                    continue;
                }

                //请求的参数
                if (annotation instanceof BodyParam) {
                    String paramName = ((BodyParam) annotation).value();
                    if(bodyMap != null && bodyMap instanceof Map) {
                        params[i] = TString.toObject(bodyMap.get(paramName).toString(), parameterTypes[i], true);
                    }
                    continue;
                }

                //请求的头
                if (annotation instanceof Head) {
                    String headName = ((Head) annotation).value();
                    params[i] = TString.toObject(request.header().get(headName), parameterTypes[i], true);
                    continue;
                }

                //请求的 Cookie
                if (annotation instanceof Cookie) {
                    String cookieValue = null;
                    String cookieName = ((Cookie) annotation).value();
                    org.voovan.http.message.packet.Cookie cookie = request.getCookie(cookieName);
                    if(cookie != null){
                        cookieValue = cookie.getValue();
                    }

                    params[i] = TString.toObject(cookieValue, parameterTypes[i], true);
                    continue;
                }

                //请求的 Body
                if (annotation instanceof Body) {
                    params[i] = TString.toObject(bodyString, parameterTypes[i], true);
                    continue;
                }

                //请求的头
                if (annotation instanceof Session) {
                    String sessionName = ((Session) annotation).value();
                    HttpSession httpSession = request.getSession();

                    if(httpSession.getAttribute(sessionName).getClass() == parameterTypes[i]){
                        params[i] = httpSession.getAttribute(sessionName);
                    }
                    continue;
                }
            }

            //没有注解的参数,按顺序处理
            if(params[i]==null) {
                String value = request.getParameter("param" + String.valueOf(i + 1));
                params[i] = TString.toObject(value, parameterTypes[i], true);
                continue;
            }

        }

        //调用方法
        return TReflect.invokeMethod(annotationObj, method, params);
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {

        try {

            //根据 Page 注解的标记设置响应的Content-Type
            response.header().put("Content-Type", HttpContentType.getHttpContentType(methodRoute.ContentType()));

            Object responseObj = invokeRouterMethod(request, response, clazz, method);
            if (responseObj != null) {
                if (responseObj instanceof String) {
                    response.write((String) responseObj);
                } else if (responseObj instanceof byte[]) {
                    response.write((byte[]) responseObj);
                } else {
                    response.header().put("Content-Type", HttpContentType.getHttpContentType(HttpContentType.JSON));
                    response.write(JSON.toJSON(responseObj));
                }
            }
        }catch(Exception e){
            throw new AnnotationRouterException("Process annotation router error. URL: " + request.protocol().getPath(), e);
        }
    }
}

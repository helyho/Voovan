package org.voovan.http.server.router;

import org.voovan.http.server.*;
import org.voovan.http.server.exception.AnnotationRouterException;
import org.voovan.http.server.router.annotation.*;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
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

    private Class clazz;
    private Method method;

    /**
     * 构造函数
     * @param clazz   Class对象
     * @param method  方法对象
     */
    public AnnotationRouter(Class clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

    /**
     * 扫描包含Router注解的类
     *
     * @param webServer   WebServer对象用于注册路由
     * @throws IOException                  IO 异常
     * @throws ReflectiveOperationException 反射异常
     */
    public static void scanRouterClassAndRegister(WebServer webServer) {
        int routeMethodNum = 0;

        try {
            //查找
            List<Class> routerClasses = TEnv.searchClassInEnv(webServer.getWebServerConfig().getScanRouterPackage(), new Class[]{Router.class});
            for (Class routerClass : routerClasses) {
                Method[] methods = routerClass.getMethods();
                Router classRouter = (Router) routerClass.getAnnotation(Router.class);
                String classRouterPath = classRouter.path().isEmpty() ? classRouter.value() : classRouter.path();
                String classRouterMethod = classRouter.method();

                //使用类名指定默认路径
                if (classRouterPath.isEmpty()) {
                    classRouterPath = "/" + routerClass.getSimpleName();
                }

                for (Method method : methods) {
                    if (method.isAnnotationPresent(Router.class)) {
                        Router methodRouter = method.getAnnotation(Router.class);
                        String methodRouterPath = methodRouter.path().isEmpty() ? methodRouter.value() : methodRouter.path();;
                        String methodRouterMethod = methodRouter.method();

                        //使用方法名指定默认路径
                        if (methodRouterPath.isEmpty()) {
                            methodRouterPath = "/" + method.getName();
                        }

                        //拼装路径
                        String routePath = classRouterPath + methodRouterPath;

                        //如果方法上的注解指定了 Method 则使用方法上的注解指定的,否则使用类上的注解指定的
                        String routeMethod = methodRouterMethod != null ? methodRouterMethod : classRouterMethod;

                        //为方法的参数准备带参数的路径
                        String paramPath = "";
                        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                        Class[] parameterTypes = method.getParameterTypes();
                        for (int i=0; i < parameterAnnotations.length; i++) {
                            Annotation[] annotations = parameterAnnotations[i];

                            if(annotations.length==0 &&
                                    parameterTypes[i] != HttpRequest.class &&
                                    parameterTypes[i] != HttpResponse.class &&
                                    parameterTypes[i] != HttpSession.class ){
                                paramPath = paramPath + "/:param"+(i+1);
                                continue;
                            }

                            for (Annotation annotation : annotations) {
                                if (annotation instanceof Param) {
                                    paramPath = paramPath + "/:" + ((Param) annotation).value();
                                }
                            }
                        }

                        //判断路由是否注册过
                        if (webServer.getHttpRouters().get(routeMethod) == null ||
                                !webServer.getHttpRouters().get(routeMethod).containsKey(routePath)) {

                            //构造注解路由器
                            AnnotationRouter annotationRouter = new AnnotationRouter(routerClass, method);

                            //注册路由,不带路径参数的路由
                            webServer.otherMethod(routeMethod, routePath, annotationRouter);
                            Logger.simple("[Router] add annotation route: " + routeMethod + " - " + routePath);
                            routeMethodNum++;

                            if(!paramPath.isEmpty()) {
                                routePath = routePath + paramPath;

                                //注册路由,带路径参数的路由
                                webServer.otherMethod(routeMethod, routePath, annotationRouter);
                                Logger.simple("[Router] add annotation route: " + routeMethod + " - " + routePath);
                                routeMethodNum++;
                            }
                        }
                    }
                }
            }

            if(routeMethodNum>0) {
                Logger.simple(TFile.getLineSeparator() + "[SYSTEM] Scan some class annotation by Router: " + routerClasses.size() +
                        ". Unregister Router method annotation by route: " + routeMethodNum + ".");
            }
        } catch (Exception e){
            Logger.error("Scan router class error.", e);
        }
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
    public static Object invokeRouterMethod(HttpRequest request, HttpResponse response, Class clazz, Method method) throws Exception {
        Object annotationObj = clazz.newInstance();
        Class[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

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
                    params[i] = TString.toObject(request.getParameter(paramName), parameterTypes[i]);
                    continue;
                }

                //请求的头
                if (annotation instanceof Head) {
                    String headName = ((Head) annotation).value();
                    params[i] = TString.toObject(request.header().get(headName), parameterTypes[i]);
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

                    params[i] = TString.toObject(cookieValue, parameterTypes[i]);
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
                params[i] = TString.toObject(value, parameterTypes[i]);
                continue;
            }

        }

        //调用方法
        return TReflect.invokeMethod(annotationObj, method, params);
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {

        try {
            Object responseObj = invokeRouterMethod(request, response, clazz, method);
            if (responseObj != null) {
                if (responseObj instanceof String) {
                    response.write((String) responseObj);
                } else if (responseObj instanceof byte[]) {
                    response.write((byte[]) responseObj);
                } else {
                    response.write(JSON.toJSON(responseObj));
                }
            }
        }catch(Exception e){
            throw new AnnotationRouterException("Process annotation router error. URL: " + request.protocol().getPath(), e);
        }
    }


}

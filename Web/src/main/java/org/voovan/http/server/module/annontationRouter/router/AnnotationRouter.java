package org.voovan.http.server.module.annontationRouter.router;

import org.voovan.http.HttpContentType;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.*;
import org.voovan.http.server.exception.AnnotationRouterException;
import org.voovan.http.server.exception.AnnotationRouterParamException;
import org.voovan.http.server.module.annontationRouter.AnnotationModule;
import org.voovan.http.server.module.annontationRouter.annotation.*;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.event.EventRunnerGroup;
import org.voovan.tools.ioc.Context;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 通过注解实现的路由
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@SuppressWarnings("ALL")
public class AnnotationRouter implements HttpRouter {
    //是否预编译路由
    public final static boolean PRE_BUILDER_ROUTER = TEnv.getSystemProperty("PrebuildRouter", false);

    public static final List<RouterInfo> ROUTER_INFO_LIST = new ArrayList<RouterInfo>();

    private static Map<Class, Object> singletonObjs = new ConcurrentHashMap<Class, Object>();

    private String urlPath;
    private String paramPath;
    private String path;
    private Class clazz;
    private Method method;
    private String methodName;
    private Router classRouter;
    private Router methodRoute;
    private AnnotationModule annotationModule;
    private DynamicFunction methodDynamicFunction;

    /**
     * 构造函数
     * @param annotationModule AnnotationModule 对象
     * @param clazz   Class对象
     * @param method  方法对象
     * @param classRouter 类上的 Route 注解
     * @param methodRoute 方法上的 Route 注解
     * @param urlPath url 路径
     * @param paramPath 带参数的 url 路径
     */
    public AnnotationRouter(AnnotationModule annotationModule, Class clazz, Method method, Router classRouter, Router methodRoute, String urlPath, String paramPath) {
        this.annotationModule = annotationModule;
        this.clazz = clazz;
        this.method = method;
        this.methodName = method.getName();
        this.classRouter = classRouter;
        this.methodRoute = methodRoute;
        this.urlPath = urlPath;
        this.paramPath = paramPath;
        this.path = urlPath + paramPath;

        if(methodRoute.async()) {
            annotationModule.increaceAsyncRouterCounter();
        }

        annotationModule.METHOD_URL_MAP.put(method, urlPath);
        annotationModule.URL_METHOD_MAP.put(urlPath, method);
        //如果是单例,则进行预实例化
        if(classRouter.singleton() && !singletonObjs.containsKey(clazz)){
            try {
                Object annotationObj  = clazz.newInstance();
                //在 IOC 中注册注解路由
                Context.addExtBean(annotationObj);
                singletonObjs.put(clazz, annotationObj);
            } catch (Exception e) {
                Logger.error("New a singleton object error", e);
            }
        }

        if(PRE_BUILDER_ROUTER) {
            methodDynamicFunction = TReflect.getMethodInvoker(clazz, method, true);
        }
    }

    public String getUrlPath() {
        return urlPath;
    }

    public String getParamPath() {
        return paramPath;
    }

    public String getPath() {
        return path;
    }

    public Class getClazz() {
        return clazz;
    }

    public Method getMethod() {
        return method;
    }

    public Router getClassRouter() {
        return classRouter;
    }

    public Router getMethodRoute() {
        return methodRoute;
    }

    public AnnotationModule getAnnotationModule() {
        return annotationModule;
    }


    public static void routerRegister(AnnotationModule annotationModule, Class routerClass) {
        WebServer webServer = annotationModule.getWebServer();
        String modulePath = annotationModule.getModuleConfig().getPath();
        modulePath = HttpDispatcher.fixRoutePath(modulePath);

        Method[] methods = routerClass.getMethods();
        Router[] annonClassRouters = (Router[]) routerClass.getAnnotationsByType(Router.class);

        //多个 Router 注解的迭代
        for (Router annonClassRouter : annonClassRouters) {
            String classRouterPath = TReflect.getAnnotationValue(annonClassRouter, "path");
            String[] classRouterMethods = annonClassRouter.method();

            //多个请求方法的迭代
            for (String classRouterMethod : classRouterMethods) {

                //使用类名指定默认路径
                if (classRouterPath.isEmpty()) {
                    //使用类名指定默认路径
                    classRouterPath = routerClass.getSimpleName();
                }

                classRouterPath = HttpDispatcher.fixRoutePath(classRouterPath);

                //扫描包含 Router 注解的方法
                for (Method method : methods) {
                    Router[] annonMethodRouters = (Router[]) method.getAnnotationsByType(Router.class);
                    if (annonMethodRouters != null) {

                        //多个 Router 注解的迭代, 一个方法支持多个路由
                        for (Router annonMethodRouter : annonMethodRouters) {
                            String methodRouterPath = TReflect.getAnnotationValue(annonMethodRouter, "path");
                            String[] methodRouterMethods = annonMethodRouter.method();

                            //多个请求方法的迭代,  一个路由支持多个 Http mehtod
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
                                methodRouterPath = HttpDispatcher.fixRoutePath(methodRouterPath);

                                //拼装 (类+方法) 路径
                                String routePath = classRouterPath + methodRouterPath;

                                //如果方法上的注解指定了 Method 则使用方法上的注解指定的,否则使用类上的注解指定的
                                String routeMethod = methodRouterMethod.isEmpty() ? classRouterMethod : methodRouterMethod;

                                //为方法的参数准备带参数的路径
                                String paramPath = "";
                                Annotation[][] paramAnnotationsArrary = method.getParameterAnnotations();
                                Class[] paramTypes = method.getParameterTypes();
                                for (int i = 0; i < paramAnnotationsArrary.length; i++) {
                                    Annotation[] paramAnnotations = paramAnnotationsArrary[i];

                                    if (paramAnnotations.length == 0 &&
                                            paramTypes[i] != HttpRequest.class &&
                                            paramTypes[i] != HttpResponse.class &&
                                            paramTypes[i] != HttpSession.class) {
                                        paramPath = paramPath + "/:param" + (i + 1);
                                        continue;
                                    }

                                    for (Annotation paramAnnotation : paramAnnotations) {
                                        if (paramAnnotation instanceof Param) {
                                            paramPath = TString.assembly(paramPath, "/:", ((Param) paramAnnotation).value());
                                        }

                                        //如果没有指定方法, 参数包含 BodyParam 注解则指定请求方法为 POST
                                        if ((paramAnnotation instanceof BodyParam || paramAnnotation instanceof Body) && routeMethod.equals(HttpStatic.GET_STRING)) {
                                            routeMethod = HttpStatic.POST_STRING;
                                        }
                                    }
                                }

                                /**
                                 * 注册路由部分代码在下面
                                 */
                                if (webServer.getHttpRouters().get(routeMethod) == null) {
                                    webServer.getHttpDispatcher().addRouteMethod(routeMethod);
                                }

                                //生成完整的路由,用来检查路由是否存在
                                routePath = HttpDispatcher.fixRoutePath(routePath);

                                ROUTER_INFO_LIST.add(new RouterInfo(routePath + paramPath, routeMethod, annonClassRouter, routerClass, annonMethodRouter, method));

                                String routeLog = null;

                                routePath = HttpDispatcher.fixRoutePath(routePath);
                                String moduleRoutePath = HttpDispatcher.fixRoutePath(modulePath + routePath);

                                if (!webServer.getHttpRouters().get(routeMethod).containsKey(moduleRoutePath)) {
                                    //构造注解路由器
                                    AnnotationRouter annotationRouter = new AnnotationRouter(annotationModule, routerClass, method,
                                            annonClassRouter, annonMethodRouter, routePath, paramPath);

                                    //1.注册路由,不带路径参数的路由
                                    annotationModule.otherMethod(routeMethod, routePath, annotationRouter);
                                    routeLog = "[HTTP] Module [" + annotationModule.getModuleConfig().getName() +
                                            "] Router: \t" + TString.rightPad(routeMethod, 8, ' ') +
                                            moduleRoutePath;

                                    //2.注册路由,带路径参数的路由
                                    if(!paramPath.isEmpty()) {
                                        String routeParamPath = null;
                                        routeParamPath = routePath + paramPath;
                                        routeParamPath = HttpDispatcher.fixRoutePath(routeParamPath);
                                        String moduleRouteParamPath = HttpDispatcher.fixRoutePath(modulePath + routeParamPath);

                                        annotationModule.otherMethod(routeMethod, routeParamPath, annotationRouter);

                                        routeLog = "[HTTP] Module [" + annotationModule.getModuleConfig().getName() +
                                                "] Router: \t" + TString.rightPad(routeMethod, 8, ' ') +
                                                moduleRouteParamPath;
                                    }
                                }

                                if(routeLog!=null) {
                                    Logger.simple(routeLog);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void webSocketRegister(AnnotationModule annotationModule, Class webSocketClass) {
        WebServer webServer = annotationModule.getWebServer();
        String modulePath = annotationModule.getModuleConfig().getPath();
        modulePath = HttpDispatcher.fixRoutePath(modulePath);

        if (TReflect.isExtends(webSocketClass, WebSocketRouter.class)) {
            WebSocket[] annonClassRouters = (WebSocket[]) webSocketClass.getAnnotationsByType(WebSocket.class);
            WebSocket annonClassRouter = annonClassRouters[0];
            String classRouterPath = TReflect.getAnnotationValue(annonClassRouter, "path");

            //使用类名指定默认路径
            if (classRouterPath.isEmpty()) {
                //使用类名指定默认路径
                classRouterPath = webSocketClass.getSimpleName();
            }

            classRouterPath = HttpDispatcher.fixRoutePath(classRouterPath);
            String moduleRoutePath = HttpDispatcher.fixRoutePath(modulePath + classRouterPath);

            if (!webServer.getWebSocketRouters().containsKey(moduleRoutePath)) {
                try {
                    annotationModule.socket(classRouterPath, (WebSocketRouter) TReflect.newInstance(webSocketClass));
                    Logger.simple("[HTTP] Module [" + annotationModule.getModuleConfig().getName() +
                            "] Router:\tWS " + TString.leftPad(moduleRoutePath, 8, ' '));
                } catch (ReflectiveOperationException e) {
                    Logger.error(e);
                }
            }
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
    public Object invokeRouterMethod(HttpRequest request, HttpResponse response, Class clazz, Method method) throws Exception {

        Object annotationObj = null;

        //如果是单例模式则使用预先初始话好的
        if(this.classRouter.singleton()){
            annotationObj = singletonObjs.get(clazz);
        } else {
            annotationObj = clazz.newInstance();
            //在 IOC 中注册注解路由
            Context.addExtBean(annotationObj);
        }

        String path = request.protocol().getPath();

        Class[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        String bodyString = "";
        Map bodyMap = null;
        if(request.body().size() > 0) {
            bodyString = request.body().getBodyString();
            if(JSON.isJSONMap(bodyString)) {
                bodyMap = (Map) JSON.parse(bodyString);
            }
        }

        //参数检查结果
        ArrayList<String> paramCheck = new ArrayList<String>();

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
                    try {
                        Object param = request.getParameter(paramName);
                        if (param == null) {
                            String defaultVal = ((Param) annotation).defaultVal();
                            if("".equals(defaultVal)) {
                                if(((Param) annotation).isRequire()) {
                                    paramCheck.add(paramName);

                                }
                                continue;
                            } else {
                                param = defaultVal;
                            }
                        }

                        params[i] = TString.toObject(param.toString(), parameterTypes[i], true);
                        continue;
                    } catch (Exception e) {
                        throw new AnnotationRouterException("Router " + path + " @Param [" + paramName + "] error: " + request.getParameters(), e).setPath(path);
                    }
                }

                //请求的参数
                if (annotation instanceof BodyParam) {
                    String paramName = ((BodyParam) annotation).value();
                    try {
                        if(bodyMap != null && bodyMap instanceof Map) {
                            Object bodyParam = bodyMap.get(paramName);
                            if(bodyParam == null) {
                                String defaultVal = ((BodyParam) annotation).defaultVal();
                                if("".equals(defaultVal)) {
                                    if(((BodyParam) annotation).isRequire()) {
                                        paramCheck.add(paramName);
                                    }
                                    continue;
                                } else {
                                    bodyParam = defaultVal;
                                }
                            }

                            Class keyType = ((BodyParam) annotation).keyType();
                            Class valueType = ((BodyParam) annotation).valueType();

                            if(TReflect.isBasicType(bodyParam.getClass())) {
                                params[i] = TString.toObject(bodyParam.toString(), parameterTypes[i], true);
                            } else if(bodyParam instanceof Map){
                                if(parameterTypes[i].equals(Map.class) &&
                                        keyType!=Object.class &&
                                        valueType != Object.class){
                                    Class[] genericType = new Class[]{keyType, valueType};
                                    params[i] = TReflect.getObjectFromMap(parameterTypes[i], (Map) bodyParam, genericType, true);
                                } else {
                                    params[i] = TReflect.getObjectFromMap(parameterTypes[i], (Map) bodyParam, true);
                                }
                            } else if(bodyParam instanceof Collection &&
                                        valueType != Object.class){
                                Map innerParam = TObject.asMap(TReflect.SINGLE_VALUE_KEY, bodyParam);
                                Class[] genericType = new Class[]{valueType};
                                params[i] = TReflect.getObjectFromMap(parameterTypes[i], (Map)innerParam, genericType, true);
                            } else {
                                params[i] = bodyParam;
                            }
                        }
                        continue;
                    } catch (Exception e) {
                        throw new AnnotationRouterException("Router " + path + " @BodyParam [" + paramName + "] error: " + bodyMap, e).setPath(path);
                    }
                }

                //请求的 Body
                if (annotation instanceof Body) {
                    try {
                        if("".equals(bodyString)) {
                            String defaultVal = ((Body) annotation).defaultVal();
                            if("".equals(defaultVal)) {
                                if(((Body) annotation).isRequire()) {
                                    paramCheck.add("@Body");
                                }
                                continue;
                            } else {
                                bodyMap = (Map) JSON.parse(defaultVal);
                            }
                        }

                        Class keyType = ((Body) annotation).keyType();
                        Class valueType = ((Body) annotation).valueType();

                        if(parameterTypes[i].equals(Map.class) &&
                                keyType!=Object.class &&
                                valueType != Object.class) {
                            Class[] genericType = new Class[]{keyType, valueType};
                            params[i] = TReflect.getObjectFromMap(parameterTypes[i], (Map) bodyMap, genericType, true);
                        } else if(bodyMap instanceof Collection &&
                                valueType != Object.class) {
                            Map innerParam = TObject.asMap(TReflect.SINGLE_VALUE_KEY, bodyMap);
                            Class[] genericType = new Class[]{valueType};
                            params[i] = TReflect.getObjectFromMap(parameterTypes[i], (Map)innerParam, genericType, true);
                        } else {
                            params[i] =  bodyMap == null ?
                                    TString.toObject(bodyString, parameterTypes[i], true) :
                                    TReflect.getObjectFromMap(parameterTypes[i], bodyMap, true);
                        }

                        continue;
                    } catch (Exception e) {
                        if(((Body) annotation).isRequire()) {
                            throw new AnnotationRouterException("Router " + path + " @Body error: " + bodyString, e).setPath(path);
                        }
                    }
                }

                //请求的头
                if (annotation instanceof Header) {
                    String headName = ((Header) annotation).value();
                    try {
                        Object headParam = request.header().get(headName);
                        if (headParam == null) {
                            String defaultVal = ((Header) annotation).defaultVal();
                            if("".equals(defaultVal)) {
                                if(((Header) annotation).isRequire()) {
                                    paramCheck.add(headName);
                                }
                                continue;
                            } else {
                                headParam = defaultVal;
                            }
                        }

                        params[i] = TString.toObject(headParam.toString(), parameterTypes[i], true);
                        continue;
                    } catch (Exception e) {
                        throw new AnnotationRouterException("Router " + path + " @Header [" + headName + "] error: " + request.header(), e).setPath(path);
                    }
                }

                //请求的 Cookie
                if (annotation instanceof Cookie) {
                    String cookieParam = null;
                    String cookieName = ((Cookie) annotation).value();
                    try {
                        org.voovan.http.message.packet.Cookie cookie = request.getCookie(cookieName);
                        if (cookie != null) {
                            cookieParam = cookie.getValue();
                        }

                        if (cookieParam == null) {
                            String defaultVal = ((Cookie) annotation).defaultVal();
                            if("".equals(defaultVal)) {
                                if(((Cookie) annotation).isRequire()) {
                                    paramCheck.add(cookieName);
                                }
                                continue;
                            } else {
                                cookieParam = defaultVal;
                            }
                        }

                        params[i] = TString.toObject(cookieParam.toString(), parameterTypes[i], true);
                        continue;
                    } catch (Exception e) {
                        throw new AnnotationRouterException("Router " + path + " @Cookie [" + cookieParam + "] error: " + request.cookies(), e).setPath(path);
                    }
                }

                //请求的头
                if (annotation instanceof Attribute) {
                    String attrName = ((Attribute) annotation).value();
                    try {
                        Object attrParam = request.getAttributes().get(attrName);
                        if(attrParam == null) {
                            String defaultVal = ((Attribute) annotation).defaultVal();
                            if("".equals(defaultVal)) {
                                if (((Attribute) annotation).isRequire()) {
                                    paramCheck.add(attrName);
                                }
                                continue;
                            } else {
                                attrParam = defaultVal;
                            }
                        }

                        if(parameterTypes[i].equals(attrParam.getClass())){
                            params[i] = attrParam;
                        } else {
                            params[i] = TString.toObject(JSON.toJSON(attrParam), parameterTypes[i], true);
                        }
                        continue;
                    } catch (Exception e) {
                        throw new AnnotationRouterException("Router " + path + " @Attribute [" + attrName + "] error: " + request.getAttributes(), e).setPath(path);
                    }
                }

                //请求的头
                if (annotation instanceof Session) {
                    String sessionName = ((Session) annotation).value();
                    HttpSession httpSession = request.getSession();

                    try {
                        Object sessionParam = httpSession.getAttribute(sessionName);
                        if(sessionParam == null) {
                            String defaultVal = ((Session) annotation).defaultVal();
                            if("".equals(defaultVal)) {
                                if (((Session) annotation).isRequire()) {
                                    paramCheck.add(sessionName);
                                }
                                continue;
                            } else {
                                sessionParam = TString.toObject(defaultVal, parameterTypes[i]);
                            }
                        }

                        if(parameterTypes[i].equals(sessionParam.getClass())){
                            params[i] = sessionParam;
                        } else {
                            params[i] = TString.toObject(JSON.toJSON(sessionParam), parameterTypes[i], true);
                        }
                        continue;
                    } catch (Exception e) {
                        throw new AnnotationRouterException("Router " + path + " @Session [" + sessionName + "] error: " + httpSession.attributes(), e).setPath(path);
                    }
                }

            }

            //没有注解的参数,按顺序处理
            if(params[i]==null) {
                try {
                    String value = request.getParameter("param" + String.valueOf(i + 1));
                    params[i] = TString.toObject(value, parameterTypes[i], true);
                    continue;
                } catch (Exception e) {
                    throw new AnnotationRouterException("Router sequential injection param " + request.getParameters().toString() + " error", e).setPath(path);
                }
            }

        }



        if(!paramCheck.isEmpty()) {
            String paramCheckMsg = "";
            for(String parmaName : paramCheck) {
                paramCheckMsg = paramCheckMsg + " " + parmaName + " = null," ;
            }

            paramCheckMsg = TString.removeSuffix(paramCheckMsg);
            paramCheckMsg = paramCheckMsg.trim();

            String requestParam = "";
            if(!request.getParameters().isEmpty()) {
                requestParam = requestParam + " paramter: " + request.getParameters().toString();
            }

            if(request.body().size()>0) {
                requestParam = requestParam + " body: " + request.getParameters().toString();
            }


            throw new AnnotationRouterParamException("Router " + path + " annotation error, param required [" + paramCheckMsg + "]" + requestParam).setPath(path).setParamCheckMsg(paramCheckMsg);
        }

        try {
            //调用方法
//             return TReflect.invokeMethod(annotationObj, method.getName(), params);
            if(methodDynamicFunction == null) {
                methodDynamicFunction = TReflect.getMethodInvoker(clazz, method, true);
            }
            return methodDynamicFunction.call(annotationObj, params);
        } catch (IllegalArgumentException e) {
            throw new AnnotationRouterException("Router method failed: \r\n [" + method + "]\r\n params" + JSON.toJSON(params), e);
        }
    }

    @Override
    public void process(HttpRequest request, HttpResponse response) throws Exception {
        EventRunnerGroup asyncEventRunnerGroup = annotationModule.getAsyncEventRunnerGroup();
        AsyncRunnerSelector asyncRunnerSelector = annotationModule.getAsyncThreadSelector();
        Supplier<Integer> selectSupplier = asyncRunnerSelector == null ? null : ()->asyncRunnerSelector.select(request);
        if(methodRoute.async()) {
            response.setAsync(true);
            asyncEventRunnerGroup.addEvent(5, ()->{
                try {
                    handler(request, response);
                    response.send();
                    response.flush();
                } catch (Exception e) {
                    Logger.errorf("Async process route ({}) failed,", e, request.protocol().getPath());
                }
            }, selectSupplier);
        } else {
            handler(request, response);
        }
    }

    public void handler(HttpRequest request, HttpResponse response) throws Exception {
        AnnotationRouterFilter annotationRouterFilter = annotationModule.getAnnotationRouterFilter();

        Object responseObj = null;
        Object fliterResult = null;

        try {
            //根据 Router 注解的标记设置响应的Content-Type
            response.header().put(HttpStatic.CONTENT_TYPE_STRING, methodRoute.contentType().getContentType());

            //过滤器前置处理
            if(annotationRouterFilter!=null) {
                fliterResult = annotationRouterFilter.beforeInvoke(request, response, this);
            }

            //null: 执行请求路由方法
            //非 null: 作为 http 请求的响应直接返回
            if(fliterResult == null) {
                responseObj = invokeRouterMethod(request, response, clazz, method);
            } else {
                responseObj = fliterResult;
            }

            //过滤器后置处理
            if(annotationRouterFilter!=null) {
                fliterResult = annotationRouterFilter.afterInvoke(request, response, this, responseObj);
                if(fliterResult!=null) {
                    responseObj = fliterResult;
                }
            }
        } catch(Exception e) {
            //过滤器拦截异常
            if(annotationRouterFilter!=null) {
                fliterResult = annotationRouterFilter.exception(request, response, this, e);
            }

            if(fliterResult !=null) {
                responseObj = fliterResult;
            } else {
                if (e.getCause() != null) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        e = (Exception) cause;
                    }
                }

                Logger.error(e);

                if (e instanceof AnnotationRouterException) {
                    throw e;
                } else if(e instanceof AnnotationRouterParamException) {
                    throw e;
                } else {
                    throw new AnnotationRouterException("Process annotation router error. URL: " + path, e);
                }
            }
        }

        if (responseObj != null && response.body().size() == 0) {
            if (responseObj instanceof String) {
                if(annotationRouterFilter!=null) {
                    responseObj = annotationRouterFilter.beforeSend(request, response, this, responseObj);
                }
                response.write((String) responseObj);
            } else if (responseObj instanceof byte[]) {
                if(annotationRouterFilter!=null) {
                    responseObj = annotationRouterFilter.beforeSend(request, response, this, responseObj);
                }
                response.write((byte[]) responseObj);
            } else {
                response.header().put(HttpStatic.CONTENT_TYPE_STRING, HttpContentType.JSON.getContentType());
                responseObj = JSON.toJSON(responseObj);
                if(annotationRouterFilter!=null) {
                    responseObj = annotationRouterFilter.beforeSend(request, response, this, responseObj);
                }
                response.write((String)responseObj);
            }

          
        }
    }
}

package org.voovan.http.server;

import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.http.server.context.HttpModuleConfig;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.tools.collection.Attributes;
import org.voovan.tools.collection.Chain;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

/**
 * WebServer的模块
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class HttpModule extends Attributes {
    private WebServer webServer;
    private HttpModuleConfig moduleConfig;
    private HttpModuleLifeCycle httpModuleLifeCycle;

    /**
     * 获取WebServer
     * @return WebServer对象
     */
    public WebServer getWebServer() {
        return webServer;
    }


    /**
     * 获取HttpModuleConfig
     * @return HttpModuleConfig对象
     */
    public HttpModuleConfig getModuleConfig() {
        return moduleConfig;
    }

    /**
     * 初始化模块操作
     * @param webServer WebServer对象
     * @param moduleConfig 模块配置对象
     */
    public void init(WebServer webServer, HttpModuleConfig moduleConfig){
        this.webServer = webServer;
        this.moduleConfig = moduleConfig;
    }

    /**
     * 获取模块的配置参数
     * @param name 参数名
     * @return 参数对象
     */
    public Object getParamters(String name){
        return moduleConfig.getParameters().get(name);
    }

    /**
     * GET 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void get(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.get(routePath,router);
    }

    /**
     * POST 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void post(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.post(routePath,router);
    }

    /**
     * HEAD 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void head(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.head(routePath,router);
    }

    /**
     * PUT 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void put(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.put(routePath,router);
    }

    /**
     * DELETE 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void delete(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.delete(routePath,router);
    }

    /**
     * TRACE 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void trace(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.trace(routePath,router);
    }

    /**
     * CONNECT 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void connect(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.connect(routePath,router);
    }

    /**
     * OPTIONS 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void options(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.options(routePath,router);
    }

    /**
     * 其他请求
     * @param method 请求方法
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void otherMethod(String method, String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        webServer.otherMethod(method,routePath,router);
    }

    /**
     * 获取过滤器链
     * @return 过滤器链
     */
    public Chain<HttpFilterConfig> filterChain(){
        return webServer.getWebServerConfig().getFilterConfigs();
    }

    /**
     * WebSocket 服务
     * @param routeRegexPath 匹配路径
     * @param router WebSocket处理句柄
     */
    public void socket(String routeRegexPath, WebSocketRouter router) {
        webServer.socket(moduleConfig.getPath() + routeRegexPath, router);
    }

    /**
     * 加载并运模块行初始化类
     */
    protected void lifeCycleInit(){
        String lifeCycleClass = this.moduleConfig.getLifeCycleClass();

        if(lifeCycleClass==null) {
            Logger.simple("[HTTP] Module ["+moduleConfig.getName()+"] None HttpMoudule lifeCycle class to load.");
            return;
        }

        if(lifeCycleClass.isEmpty()){
            Logger.simple("[HTTP] Module ["+moduleConfig.getName()+"] None HttpMoudule lifeCycle class to load.");
            return;
        }

        try {

            Class clazz = Class.forName(lifeCycleClass);
            if(TReflect.isImp(clazz, HttpModuleLifeCycle.class)){
                httpModuleLifeCycle = (HttpModuleLifeCycle)TReflect.newInstance(clazz);
                httpModuleLifeCycle.init(this);
            }else{
                Logger.warn("["+moduleConfig.getName()+"] The HttpModule lifeCycle class " + lifeCycleClass + " is not a class implement by " + HttpModuleLifeCycle.class.getName());
            }
        } catch (Exception e) {
            Logger.error("["+moduleConfig.getName()+"] Initialize HttpModule lifeCycle class error: " + e);
        }
    }

    /**
     * 加载并运模块行初始化类
     */
    protected void lifeCycleDestory(){
        if(httpModuleLifeCycle!=null) {
            try {
                httpModuleLifeCycle.destory(this);
            } catch (Exception e) {
                Logger.error("[" + moduleConfig.getName() + "] Initialize HttpModule lifeCycle class error: " + e);
            }
        }
    }

    /**
     * 安装模块至 WebServer
     */
    public abstract void install();

    /**
     * 安装模块至 WebServer
     */
    public abstract void unInstall();
}

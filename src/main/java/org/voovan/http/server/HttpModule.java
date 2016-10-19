package org.voovan.http.server;

import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.http.server.context.HttpModuleConfig;
import org.voovan.tools.Chain;

/**
 * HttpServer的模块
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class HttpModule {
    private WebServer httpServer;
    private HttpModuleConfig moduleConfig;

    public HttpModule(){

    }

    /**
     * 初始化模块操作
     * @param httpServer httpServer对象
     * @param moduleConfig 模块配置对象
     */
    public void init(WebServer httpServer, HttpModuleConfig moduleConfig){
        this.httpServer = httpServer;
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
        httpServer.get(routePath,router);
    }

    /**
     * POST 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void post(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        httpServer.post(routePath,router);
    }

    /**
     * HEAD 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void head(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        httpServer.head(routePath,router);
    }

    /**
     * PUT 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void put(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        httpServer.put(routePath,router);
    }

    /**
     * DELETE 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void delete(String routeRegexPath, HttpRouter router) {
       String routePath = moduleConfig.getPath()+routeRegexPath;
       httpServer.delete(routePath,router);
    }

    /**
     * TRACE 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void trace(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        httpServer.trace(routePath,router);
    }

    /**
     * CONNECT 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void connect(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        httpServer.connect(routePath,router);
    }

    /**
     * OPTIONS 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void options(String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        httpServer.options(routePath,router);
    }

    /**
     * 其他请求
     * @param method 请求方法
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     */
    public void otherMethod(String method, String routeRegexPath, HttpRouter router) {
        String routePath = moduleConfig.getPath()+routeRegexPath;
        httpServer.otherMethod(method,routePath,router);
    }

    /**
     * 获取过滤器链
     * @return 过滤器链
     */
    public Chain<HttpFilterConfig> filterChain(){
        return httpServer.getWebServerConfig().getFilterConfigs();
    }

    /**
     * 安装模块至 WebServer
     */
    public abstract void install();
}

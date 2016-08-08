package org.voovan.http.server;

import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.tools.Chain;

import java.util.Map;

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
    private HttpServer httpServer;
    private Map<String,Object> paramters;

    public HttpModule(){

    }

    /**
     * 初始化模块操作
     * @param httpServer httpServer对象
     * @param paramters 模块配置参数
     */
    public void init(HttpServer httpServer,Map<String,Object> paramters){
        this.httpServer = httpServer;
        this.paramters = paramters;
    }

    /**
     * 获取模块的配置参数
     * @param name
     * @return
     */
    public Object getParamters(String name){
        return paramters.get(name);
    }

    /**
     * GET 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void get(String routeRegexPath, HttpRouter router) {
        httpServer.get(routeRegexPath,router);
    }

    /**
     * POST 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void post(String routeRegexPath, HttpRouter router) {
        httpServer.post(routeRegexPath,router);
    }

    /**
     * HEAD 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void head(String routeRegexPath, HttpRouter router) {
        httpServer.head(routeRegexPath,router);
    }

    /**
     * PUT 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void put(String routeRegexPath, HttpRouter router) {
        httpServer.put(routeRegexPath,router);
    }

    /**
     * DELETE 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void delete(String routeRegexPath, HttpRouter router) {
       httpServer.delete(routeRegexPath,router);
    }

    /**
     * TRACE 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void trace(String routeRegexPath, HttpRouter router) {
        httpServer.trace(routeRegexPath,router);
    }

    /**
     * CONNECT 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void connect(String routeRegexPath, HttpRouter router) {
        httpServer.connect(routeRegexPath,router);
    }

    /**
     * OPTIONS 请求
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void options(String routeRegexPath, HttpRouter router) {
        httpServer.options(routeRegexPath,router);
    }

    /**
     * 其他请求
     * @param method 请求方法
     * @param routeRegexPath 匹配路径
     * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
    public void otherMethod(String method, String routeRegexPath, HttpRouter router) {
        httpServer.otherMethod(method,routeRegexPath,router);
    }

    /**
     * 获取过滤器链
     * @return 过滤器链
     */
    public Chain<HttpFilterConfig> filterChain(){
        return httpServer.getWebServerConfig().getFilterConfigs();
    }

    /**
     * 安装模块至 HttpServer
     */
    public abstract void install();
}

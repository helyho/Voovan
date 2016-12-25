package org.voovan.http.server.context;

import org.voovan.http.server.HttpRouter;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.Map;

/**
 * 过路由处理器信息对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpRouterConfig {
    private String name;
    private String route;
    private String className;
    private String method;
    private HttpRouter httpRouter;

    /**
     * 构造函数
     *
     * @param configMap 过滤去定义 Map
     */
    public HttpRouterConfig(Map<String, Object> configMap) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            if ("Name".equalsIgnoreCase(entry.getKey())) {
                this.name = (String) entry.getValue();
            } else if ("Route".equalsIgnoreCase(entry.getKey())) {
                this.route = (String) entry.getValue();
            } else if ("ClassName".equalsIgnoreCase(entry.getKey())) {
                this.className = (String) entry.getValue();
            } else if ("Method".equalsIgnoreCase(entry.getKey())) {
                this.method = (String) entry.getValue();
            }
        }
    }

    /**
     * 构造函数
     */
    public HttpRouterConfig() {

    }

    /**
     * 获取路由名称
     * @return 路由名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置路由名称
     * @param name 路由名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取路由路径
     * @return 路由路径
     */
    public String getRoute() {
        return TObject.nullDefault(route,"/");
    }

    /**
     * 设置路由路径名称
     * @param route 路由路径
     */
    public void setRoute(String route) {
        this.route = route;
    }

    /**
     * 获取过路由处理类名
     * @return 路由处理类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置路由处理名
     * @param className 路由处理类名
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取Http请求方法
     * @return Http请求方法
     */
    public String getMethod() {
        return method;
    }

    /**
     * 设置Http请求方法
     * @param method Http请求方法
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * 获取HttpBuizFilter过滤器实例
     *
     * @return 过滤器实例
     */
    public HttpRouter getHttpRouterInstance() {
        try {
            //单例模式
            if (httpRouter == null) {
                httpRouter = TReflect.newInstance(className);
            }
            return httpRouter;
        } catch (ReflectiveOperationException e) {
            Logger.error("[ERROR] New HttpRouter ["+className+"] error.");
            return null;
        }
    }
}

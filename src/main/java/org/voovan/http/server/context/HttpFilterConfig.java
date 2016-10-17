package org.voovan.http.server.context;

import org.voovan.http.server.HttpFilter;
import org.voovan.tools.TReflect;
import org.voovan.tools.log.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 过滤器配置信息对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpFilterConfig {
    private String name;
    private String className;
    private Map<String, Object> paramters = new HashMap<String, Object>();
    private HttpFilter httpFilter;

    /**
     * 构造函数
     *
     * @param configMap 过滤去定义 Map
     */
    public HttpFilterConfig(Map<String, Object> configMap) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            if ("Name".equalsIgnoreCase(entry.getKey())) {
                this.name = (String) entry.getValue();
            } else if ("ClassName".equalsIgnoreCase(entry.getKey())) {
                this.className = (String) entry.getValue();
            } else {
                paramters.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 构造函数
     * @param name   过滤器名称
     * @param className  过滤器类
     * @param parameters  过滤器参数
     */
    public HttpFilterConfig(String name, String className, Map<String, Object> parameters){
        this.name = name;
        this.className = className;
        if(parameters!=null) {
            this.paramters = parameters;
        }
    }

    /**
     * 构造函数
     */
    public HttpFilterConfig() {

    }

    /**
     * 获取过滤器名称
     * @return 过滤器名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置过滤器名称
     * @param name 过滤器名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取过滤器类名
     * @return 过滤器类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置过滤器类名
     * @param className 过滤器类名
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 获取过滤器的参数,在过滤器定义的时候
     *
     * @return 过滤器参数
     */
    public Map<String, Object> getParameters() {
        return paramters;
    }

    /**
     * 获取过滤器的参数,在过滤器定义的时候
     * @param name 过滤器参数名
     * @return 过滤器参数值
     */
    public Object getParameter(String name) {
        return paramters.get(name);
    }

    /**
     * 获取HttpBuizFilter过滤器实例
     *
     * @return 过滤器实例
     */
    public HttpFilter getHttpFilterInstance() {
        try {
            //单例模式
            if (httpFilter == null) {
                httpFilter = TReflect.newInstance(className);
            }
            return httpFilter;
        } catch (ReflectiveOperationException e) {
            Logger.error("[ERROR] New HttpFilter ["+className+"] error.");
            return null;
        }
    }

    /**
     * 新的过滤器配置对象
     * @param name   过滤器名称
     * @param className  过滤器类
     * @param parameters  过滤器参数
     * @return 过滤器配置对象
     */
    public static HttpFilterConfig newInstance(String name, String className, Map<String, Object> parameters){
        return new HttpFilterConfig(name,className,parameters);
    }

    /**
     * 新的过滤器配置对象
     * @param name   过滤器名称
     * @param clazz  过滤器类
     * @param parameters  过滤器参数
     * @return 过滤器配置对象
     */
    public static HttpFilterConfig newInstance(String name, Class clazz, Map<String, Object> parameters){
        return new HttpFilterConfig(name,clazz.getCanonicalName(),parameters);
    }
}

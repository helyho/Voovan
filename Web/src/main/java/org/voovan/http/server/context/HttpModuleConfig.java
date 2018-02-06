package org.voovan.http.server.context;

import org.voovan.http.server.HttpModule;
import org.voovan.http.server.WebServer;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.HashMap;
import java.util.Map;

/**
 * Http模块配置类
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpModuleConfig {
    private String name;
    private String path;
    private String className;
    private Map<String, Object> paramters = new HashMap<String, Object>();
    private String lifeCycleClass = null;
    private HttpModule httpModule;

    public HttpModuleConfig(Map<String, Object> configMap) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            if ("Name".equalsIgnoreCase(entry.getKey())) {
                this.name = (String) entry.getValue();
            }else if ("Path".equalsIgnoreCase(entry.getKey())) {
                this.path = (String) entry.getValue();
            }else if ("ClassName".equalsIgnoreCase(entry.getKey())) {
                this.className = (String) entry.getValue();
            }else if ("LifeCycleClass".equalsIgnoreCase(entry.getKey())) {
                this.lifeCycleClass = (String) entry.getValue();
            } else {
                paramters.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return TObject.nullDefault(path,"/");
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getClassName() {
        return className;
    }

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

    public String getLifeCycleClass() {
        return lifeCycleClass;
    }

    public void setLifeCycleClass(String lifeCycleClass) {
        this.lifeCycleClass = lifeCycleClass;
    }

    /**
     * 获取HttpBuizFilter过滤器实例
     * @param webServer WebServer对象
     * @return 过滤器实例
     */
    public HttpModule getHttpModuleInstance(WebServer webServer) {
        try {
            //单例模式
            if (httpModule == null) {
                httpModule = TReflect.newInstance(className);
                httpModule.init(webServer,this);
            }
            return httpModule;
        } catch (ReflectiveOperationException e) {
            Logger.error("[ERROR] New HttpModule ["+className+"] error.");
            e.printStackTrace();
            return null;
        }
    }

}

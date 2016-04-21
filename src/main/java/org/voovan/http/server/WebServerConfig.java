package org.voovan.http.server;

import org.voovan.tools.Chain;
import org.voovan.tools.TReflect;
import org.voovan.tools.log.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * WebServer 配置类
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class WebServerConfig {
    private String host;
    private int port;
    private int timeout;
    private String contextPath;
    private String characterSet;
    private String sessionContainer;
    private int sessionTimeout;
    private int keepAliveTimeout;
    private boolean gzip;
    private String certificateFile;
    private String certificatePassword;
    private String keyPassword;
    private boolean accessLog;
    private boolean monitor;

    private Chain<FilterConfig> filterConfigs = new Chain<FilterConfig>();

    protected void setHost(String host) {
        this.host = host;
    }

    protected void setPort(int port) {
        this.port = port;
    }

    protected void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    protected void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    protected void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    protected void setSessionContainer(String sessionContainer) {
        this.sessionContainer = sessionContainer;
    }

    protected void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    protected void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public String getSessionContainer() {
        return sessionContainer;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public boolean isGzip() {
        return gzip;
    }

    public void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public String getCertificateFile() {
        return certificateFile;
    }

    public void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public boolean isAccessLog() {
        return accessLog;
    }

    public void setAccessLog(boolean accessLog) {
        this.accessLog = accessLog;
    }

    public boolean isMonitor() {
        return monitor;
    }

    public void setMonitor(boolean monitor) {
        this.monitor = monitor;
    }

    public Chain<FilterConfig> getFilterConfigs() {
        return filterConfigs;
    }

    /**
     * 增加一个过滤器
     * 其中 name 和 className 会被初始化成过滤器的属性,其他会被初始化成过滤器的参数
     *
     * @param configMap
     */
    public void addFilterConfig(Map<String, Object> configMap) {
        filterConfigs.addLast(new FilterConfig(configMap));
        filterConfigs.rewind();
    }

    /**
     * 使用列表初始话过滤器链
     *
     * @param filterInfoList
     */
    public void addAllFilterConfigs(List<Map<String, Object>> filterInfoList) {
        for (Map<String, Object> filterConfigMap : filterInfoList) {
            this.addFilterConfig(filterConfigMap);
        }
    }

    public String toString(){
        try {
            Map<Field, Object> fieldValues = TReflect.getFieldValues(this);
            String str = "";
            for(Entry<Field,Object> entry : fieldValues.entrySet()){
                str+=entry.getKey().getName()+":\t\t"+entry.getValue()+"\r\n";
            }
            return str;
        } catch (ReflectiveOperationException e) {
            Logger.error(e);
        }
        return null;
    }

    /**
     * 构造一个空的实例
     * @return
     */
    public static FilterConfig newFilterConfig(){
        return new FilterConfig();
    }

    /**
     * 过滤器配置信息对象,内联对象
     *
     * @author helyho
     *         <p>
     *         Voovan Framework.
     *         WebSite: https://github.com/helyho/Voovan
     *         Licence: Apache v2 License
     */
    public static class FilterConfig {
        private String name;
        private String className;
        private Map<String, Object> paramters = new HashMap<String, Object>();
        private HttpBizFilter httpBizFilter;

        /**
         * 构造函数
         *
         * @param configMap
         */
        public FilterConfig(Map<String, Object> configMap) {
            for (Entry<String, Object> entry : configMap.entrySet()) {
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
         */
        public FilterConfig() {

        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
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
         * @return
         */
        public Map<String, Object> getParameters() {
            return paramters;
        }

        /**
         * 获取HttpBuizFilter过滤器实例
         *
         * @return
         * @throws ReflectiveOperationException
         */
        protected HttpBizFilter getBizFilter() throws ReflectiveOperationException {
            //单例模式
            if (httpBizFilter == null) {
                httpBizFilter = TReflect.newInstance(className);
            }
            return httpBizFilter;
        }


    }
}

package org.voovan.http.server;

import org.voovan.tools.Chain;
import org.voovan.tools.TReflect;
import org.voovan.tools.log.Logger;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

/**
 * WebServer 配置类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebServerConfig {
    private String host             = "0.0.0.0";
    private int port                = 28080;
    private int timeout             = 30;
    private String contextPath      = "WEBAPP";
    private boolean MatchRouteIgnoreCase = false;
    private String characterSet     = "UTF-8";
    private String sessionContainer = "java.util.Hashtable";
    private int sessionTimeout      = 30;
    private int keepAliveTimeout    = 60;
    private boolean accessLog       = true;
    private boolean monitor         = false;
    private boolean gzip            = true;
    private String certificateFile;
    private String certificatePassword;
    private String keyPassword;


    private Chain<HttpFilterConfig> filterConfigs = new Chain<HttpFilterConfig>();

    private List<HttpRouterConfig> routerConfigs = new Vector<HttpRouterConfig>();

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

    public boolean isMatchRouteIgnoreCase() {
        return MatchRouteIgnoreCase;
    }

    protected void setMatchRouteIgnoreCase(boolean matchRouteIgnoreCase) {
        MatchRouteIgnoreCase = matchRouteIgnoreCase;
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

    protected void setGzip(boolean gzip) {
        this.gzip = gzip;
    }

    public String getCertificateFile() {
        return certificateFile;
    }

    protected void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    protected void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    protected void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public boolean isAccessLog() {
        return accessLog;
    }

    protected void setAccessLog(boolean accessLog) {
        this.accessLog = accessLog;
    }

    public boolean isMonitor() {
        return monitor;
    }

    protected void setMonitor(boolean monitor) {
        this.monitor = monitor;
    }

    public Chain<HttpFilterConfig> getFilterConfigs() {
        return filterConfigs;
    }

    public List<HttpRouterConfig> getRouterConfigs() {
        return routerConfigs;
    }

    /**
     * 增加一个过滤器
     * 其中 name 和 className 会被初始化成过滤器的属性,其他会被初始化成过滤器的参数
     *
     * @param configMap 过滤器配置 Map
     */
    public void addFilterConfig(Map<String, Object> configMap) {
        HttpFilterConfig httpFilterConfig = new HttpFilterConfig(configMap);
        filterConfigs.addLast(httpFilterConfig);
        Logger.debug("Load HttpFilter "+httpFilterConfig.getName()+
                " by <"+ httpFilterConfig.getClassName()+">");
        filterConfigs.rewind();
    }

    /**
     * 使用列表初始话路由处理器
     *
     * @param routerInfoList  路由处理器信息列表
     */
    public void addRouterByConfigs(List<Map<String, Object>>  routerInfoList) {
        for (Map<String, Object> routerInfoMap : routerInfoList) {
            HttpRouterConfig httpRouterConfig = new HttpRouterConfig(routerInfoMap);
           routerConfigs.add(httpRouterConfig);
            Logger.debug("Load HttpRouter "+httpRouterConfig.getMethod()+
                    " on  ["+httpRouterConfig.getRoute()+"] by <"+ httpRouterConfig.getClassName()+">");
        }
    }
    /**
     * 使用列表初始话过滤器链
     *
     * @param filterInfoList 过滤器信息列表
     */
    public void addFilterByConfigs(List<Map<String, Object>> filterInfoList) {
        for (Map<String, Object> filterConfigMap : filterInfoList) {
            this.addFilterConfig(filterConfigMap);
        }
    }

    @Override
    public String toString(){
        try {
            Map<Field, Object> fieldValues = TReflect.getFieldValues(this);
            StringBuilder str = new StringBuilder();
            for(Entry<Field,Object> entry : fieldValues.entrySet()){
                str.append(entry.getKey().getName());
                str.append(":\t\t");
                str.append(entry.getValue());
                str.append("\r\n");
            }
            return str.toString();
        } catch (ReflectiveOperationException e) {
            Logger.error(e);
        }
        return "ReflectiveOperationException error by TReflect.getFieldValues Method. ";
    }

    /**
     * 构造一个空的实例
     * @return 过滤器对象
     */
    public static HttpFilterConfig newFilterConfig(){
        return new HttpFilterConfig();
    }
}

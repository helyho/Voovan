package org.voovan.http.monitor;

import org.voovan.http.server.HttpServer;
import org.voovan.http.server.WebServerConfig;

import java.io.IOException;

/**
 * 监控器类
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Monitor {

    public static void addMonitorFilter(HttpServer httpServer){
        WebServerConfig config = httpServer.getWebServerConfig();
        WebServerConfig.FilterConfig filterConfig = WebServerConfig.newFilterConfig();
        filterConfig.setName("MonitorFilter");
        filterConfig.setClassName("org.voovan.http.monitor.HttpMonitorFilter");
        config.getFilterConfigs().add(filterConfig);
    }

    public static void installMonitor(HttpServer httpServer){
        httpServer.get("/VoovanMonitor/:Type/:Param1",new MonitorHandler());
        httpServer.get("/VoovanMonitor/:Type",new MonitorHandler());
        httpServer.get("/VoovanMonitor/:Type/:Param1/:Param2",new MonitorHandler());
        addMonitorFilter(httpServer);
    }

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.newInstance();
        installMonitor(httpServer);
        httpServer.serve();
    }


}

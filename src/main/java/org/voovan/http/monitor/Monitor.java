package org.voovan.http.monitor;

import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.http.server.HttpModule;
import org.voovan.http.server.HttpServer;

import java.util.Map;

/**
 * 监控器类
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Monitor extends HttpModule {

    @Override
    public void install() {
        get("/VoovanMonitor/:Type/:Param1",new MonitorHandler());
        get("/VoovanMonitor/:Type",new MonitorHandler());
        get("/VoovanMonitor/:Type/:Param1/:Param2",new MonitorHandler());
        filterChain().addFirst(HttpFilterConfig.newInstance("MonitorFilter",HttpMonitorFilter.class,null));
    }
}

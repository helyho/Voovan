package org.voovan.http.monitor;

import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.http.server.HttpModule;
import org.voovan.http.server.HttpServer;
import org.voovan.http.server.context.HttpModuleConfig;

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
        get("/:Type/:Param1",new MonitorHandler());
        get("/:Type",new MonitorHandler());
        get("/:Type/:Param1/:Param2",new MonitorHandler());
        filterChain().addFirst(HttpFilterConfig.newInstance("MonitorFilter",HttpMonitorFilter.class,null));
    }
}

package org.voovan.http.server.module.monitor;

import org.voovan.http.server.HttpModule;
import org.voovan.http.server.context.HttpFilterConfig;

import java.util.List;
import java.util.Vector;

/**
 * 监控器类
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MonitorModule extends HttpModule {

    @Override
    public void install() {
        Vector<String> allowIPAddress = new Vector<String>();
        allowIPAddress.addAll((List<String>)getParamters("AllowIPAddress"));
        MonitorGlobal.ALLOW_IP_ADDRESS = allowIPAddress;

        //注册路由
        this.otherMethod("MONITOR", "/:Type/:Param1",new MonitorRouter());
        this.otherMethod("MONITOR", "/:Type",new MonitorRouter());
        this.otherMethod("MONITOR", "/:Type/:Param1/:Param2",new MonitorRouter());

        //注册过滤器,用于抓取分析数据
        filterChain().addFirst(HttpFilterConfig.newInstance("MonitorFilter",HttpMonitorFilter.class,null));
    }
}

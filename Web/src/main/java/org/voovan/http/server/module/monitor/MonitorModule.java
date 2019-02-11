package org.voovan.http.server.module.monitor;

import org.voovan.http.server.HttpModule;
import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.http.server.router.OptionsRouter;
import org.voovan.tools.TPerformance;

import java.util.List;

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
        List<String> allowIPAddress = TPerformance.getLocalIpAddrs();
        allowIPAddress.addAll((List<String>)getParamters("AllowIPAddress"));
        MonitorGlobal.ALLOW_IP_ADDRESS = allowIPAddress;

        //注册路由
        this.otherMethod("MONITOR", "/:Type/:Param1",new MonitorRouter());
        this.otherMethod("MONITOR", "/:Type",new MonitorRouter());
        this.otherMethod("MONITOR", "/:Type/:Param1/:Param2",new MonitorRouter());
        this.options("/*", new OptionsRouter("MONITOR", "*", "auth-token"));
        //注册过滤器,用于抓取分析数据
        filterChain().add(HttpFilterConfig.newInstance("MonitorFilter",HttpMonitorFilter.class,null));
    }

    @Override
    public void unInstall() {

    }
}

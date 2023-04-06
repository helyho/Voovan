package org.voovan.test.tools.ioc;

import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.annotation.Destory;
import org.voovan.tools.ioc.annotation.Initialize;
import org.voovan.tools.ioc.annotation.Value;
import org.voovan.tools.log.Logger;

import java.util.List;
import java.util.Map;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@Bean(name="IOCExt", lazy = true)
public class IOCExt {
    @Value
    private IOC1 ioc1;
    @Value("string")
    private String str;

    @Value("Filters[0].ClassName")
    private String filter0ClassName;

    @Value
    private List list;
    @Value(anchor = "ExtMap", required = false)
    private Map map;

    @Initialize
    public void init(@Value("Filters[0].Name") String filterName){
        Logger.simple("IOCExt init " + filterName);
    }

    @Destory
    public void destory(@Value("Host") String host) {
        Logger.simple("IOCExt destory" + host);
    }

    @Bean("ExtServerName")
    public String getServerName(@Value("ServerName") String serverName){
        Logger.simple("extServerName: " + serverName);
        return serverName;
    }
}

package org.voovan.test.tools.ioc;

import org.voovan.tools.ioc.annotation.Bean;
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
@Bean(name="IOCExt", lazy = true, init = "init", destory = "destory")
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

    public void init(){
        Logger.simple("IOCExt init");
    }

    public void destory() {
        Logger.simple("IOCExt destory");
    }

    @Bean("ExtServerName")
    public String getServerName(@Value("ServerName") String serverName){
        Logger.simple("extServerName: " + serverName);
        return serverName;
    }
}

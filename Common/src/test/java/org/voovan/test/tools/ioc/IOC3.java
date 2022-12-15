package org.voovan.test.tools.ioc;

import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.annotation.Primary;
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
@Bean(name="IOC3", lazy = true, init = "init", destory = "destory", singleton = false)
public class IOC3 {
    @Value
    private IOC1 ioc1;
    @Value("string")
    private String str;

    @Value
    private List list;
    @Value(anchor = "Map", required = false)
    private Map map;

    public void init(){
        Logger.simple("IOC3 init");
    }

    public void destory() {
        Logger.simple("IOC3 destory");
    }
}

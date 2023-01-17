package org.voovan.test.tools.ioc;

import org.voovan.tools.ioc.annotation.Bean;
import org.voovan.tools.ioc.annotation.Primary;
import org.voovan.tools.ioc.annotation.Value;

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
@Bean(name="IOC2", lazy = true)
public class IOC2 {
    @Value
    private IOC1 ioc1;
    @Value("string")
    private String str;

    @Value
    private List list;
    @Value(anchor = "Map", required = false)
    private Map map;

    @Bean
    @Primary
    public List getList(@Value List data){
        data.add("item3");
        return data;
    }

    @Primary
    @Bean("ServerName")
    public String getFilters(@Value("Filters") List data, @Value("ServerName") String serverName){
        return serverName + "->" + data;
    }

    @Bean(value="IOC3Method", lazy = true)
    public IOC3 getIOC3(){
        System.out.println("ioc3 method");
        return new IOC3();
    }
}

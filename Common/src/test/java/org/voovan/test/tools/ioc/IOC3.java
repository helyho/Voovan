package org.voovan.test.tools.ioc;

import org.voovan.tools.ioc.annotation.*;
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
@Bean(useClassName = true, lazy = true, singleton = true)
public class IOC3 {

    @Value("TestList")
    private List<Item> itemList;

    @Value("TestMap")
    private Map<String, Item> itemMap;

    @Value
    private IOC1 ioc1;
    @Value("string")
    private String str;

    @Value("Filters[0].ClassName")
    private String filter0ClassName;


    @Value
    private List list;
    @Value(anchor = "Map", required = false)
    private Map map;

    @Bean("beanValue")
    public void test(@Value("TestList") List<Item> itmes) {
        Logger.simple(itmes.get(0).getClass());
    }

    @Initialize
    public void init(){
        Logger.simple("IOC3 init");
    }

    @Destory
    public void destory(@Value("Host") String host) {
        Logger.simple("IOC3 destory " + host);
    }

}

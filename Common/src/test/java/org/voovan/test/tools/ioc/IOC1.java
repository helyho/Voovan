package org.voovan.test.tools.ioc;

import org.voovan.tools.TObject;
import org.voovan.tools.ioc.annotation.Bean;
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
@Bean
public class IOC1 {
    @Value("ServerName")
    private String str;
    private int integer;

    private Map map = TObject.asMap("key1", "value1");

    private List list = TObject.asList("item1", "item2");

    public IOC1() {
        this.str = "string";
        this.integer = 9999;
    }

    @Bean("string")
    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public int getInteger() {
        return integer;
    }

    public void setInteger(int integer) {
        this.integer = integer;
    }

    @Bean("Map")
    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    @Bean
    public List getList() {
        return list;
    }

    public void setList(List list) {
        this.list = list;
    }
}

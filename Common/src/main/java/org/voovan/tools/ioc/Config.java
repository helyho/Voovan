package org.voovan.tools.ioc;

import org.voovan.tools.exception.IOCException;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置文件类
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Config {

    public Map<String, Object> config;

    public Config(){
        config = new LinkedHashMap<>();
    }
    public Config(URL url){
        init(url);
    }

    public void init(URL url) {
        config = JSON.toObject(url, Map.class, true, true, true);

        if(config == null) {
            config = new LinkedHashMap<>();
            Logger.warnf("Load IOC config file: \"{}\" failed", url.toString());
        } else if (config!=null) {
            Logger.debug(config);
            if(!(config instanceof Map)) {
                throw new IOCException("ConfigFile must be a Map style file");
            } else {
                Logger.simplef("[FRAMEWRORK] IOC Context load from: {}", url.toString());
            }
        }
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}

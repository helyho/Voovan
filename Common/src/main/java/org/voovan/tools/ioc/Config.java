package org.voovan.tools.ioc;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.net.MalformedURLException;
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
        init(null);
    }
    public Config(URL url){
        init(url);
    }

    private URL getDefaulConfigURL(){
        String iocConfig = TEnv.getSystemProperty("IocConfig", String.class);
        String applicationConfigFile = "conf/application.json";
        if (TFile.fileExists("conf/application.hcl")) {
            applicationConfigFile = "conf/application.hcl";
        }
        iocConfig = iocConfig == null ? TEnv.getEnv("VOOVAN_IOC_CONFIG", applicationConfigFile) : iocConfig;

        // 判断是否是 url 形式, 如果不是则进行转换
        if (TString.regexMatch(iocConfig, "^[a-z,A-Z]*?://") == 0) {
            iocConfig = "file://" + TFile.getSystemPath(iocConfig);
        }

        try {
            return new URL(iocConfig);
        } catch (MalformedURLException e) {
            throw new IOCException("Load IOC config failed", e);
        }
    }

    public void init(URL url) {
        if(url == null) {
           url = getDefaulConfigURL(); 
        }

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

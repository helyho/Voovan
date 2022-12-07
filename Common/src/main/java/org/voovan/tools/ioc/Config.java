package org.voovan.tools.ioc;

import org.voovan.tools.TFile;
import org.voovan.tools.exception.IOCException;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONPath;

import java.io.File;
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
    public static String configFile = "./ioc.json";

    public Map<String, Object> config;

    public String name;

    public Config() {
        this.name = "config";
        init();
    }


    public Config(String configFile) {
        this.configFile = configFile;
        init();
    }

    public void init() {
        String fileName = TFile.getFileName(configFile);
        this.name = fileName.substring(0, fileName.indexOf('.'));
        String content = new String(TFile.loadResource(configFile));
        config = (Map<String, Object>)JSON.parse(content);
        if(!(config instanceof Map)) {
            throw new IOCException("ConfigFile must be a Map style file");
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}

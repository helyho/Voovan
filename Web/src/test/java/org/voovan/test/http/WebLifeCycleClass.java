package org.voovan.test.http;

import org.voovan.http.server.WebServer;
import org.voovan.http.server.WebServerLifeCycle;
import org.voovan.tools.ioc.annotation.Value;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public class WebLifeCycleClass implements WebServerLifeCycle {
    @Value("Web.Host")
    private String host;
    private int port;
    @Override
    public void init(WebServer webServer) {
        Logger.simple("Init messsage" + host + ":" + port);
    }

    @Override
    public void destory(WebServer webServer) {
        Logger.simple("Destory messsage");
    }
}

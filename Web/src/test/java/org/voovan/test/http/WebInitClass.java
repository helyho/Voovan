package org.voovan.test.http;

import org.voovan.http.server.WebServer;
import org.voovan.http.server.WebServerInit;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public class WebInitClass implements WebServerInit{
    @Override
    public void init(WebServer webServer) {
        Logger.info("Init messsage");
    }
}

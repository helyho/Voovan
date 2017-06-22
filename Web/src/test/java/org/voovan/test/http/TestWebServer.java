package org.voovan.test.http;

import com.sun.tools.javadoc.Start;
import org.voovan.http.server.WebServer;
import org.voovan.http.server.context.WebServerConfig;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TestWebServer {

    public static void main(String[] args) {
        WebServerConfig wsc = new WebServerConfig();
        wsc.setGzip(true);
        wsc.setAccessLog(false);
        wsc.setPort(8800);
        wsc.setTimeout(10000);
        wsc.setContextPath(Start.class.getResource("/").getPath());
        WebServer ws = WebServer.newInstance(wsc);
        ws.get("/", (req1,resp1)->{
            resp1.write("this is test1 Message.");
        });
        ws.serve();
        System.out.println("已经启动了...");
    }
}

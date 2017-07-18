package org.voovan.test.http;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.HttpRouter;
import org.voovan.http.server.WebServer;

/**
 * Http简单服务测试类
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SimpleWebServerDemo {
    public static void main(String[] args) {
        WebServer.newInstance(20001).get("/test", new HttpRouter() {
            @Override
            public void process(HttpRequest request, HttpResponse response) throws Exception {
                response.write("this is test Message.");
            }
        }).get("/test1", new HttpRouter() {
            @Override
            public void process(HttpRequest request, HttpResponse response) throws Exception {
                response.write("this is test1 Message.");
            }
        }).serve();
    }
}
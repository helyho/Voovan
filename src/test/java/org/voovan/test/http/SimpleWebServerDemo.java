package org.voovan.test.http;

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
        WebServer.newInstance(20001).get("/test",(req, resp)->{
            resp.write("this is test Message.");
        }).get("/test1",(req1,resp1)->{
            resp1.write("this is test1 Message.");
        }).serve();
    }
}
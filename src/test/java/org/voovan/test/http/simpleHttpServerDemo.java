package org.voovan.test.http;

import org.voovan.http.server.HttpServer;

import java.io.IOException;

/**
 * Http简单服务测试类
 *
 * @author helyho
 *         <p>
 *         Java Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class simpleHttpServerDemo {
    public static void main(String[] args) {
        HttpServer.newInstance().get("/test",(req,resp)->{
            resp.write("this is test Message.");
        }).serve();
    }
}
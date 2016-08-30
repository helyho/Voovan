package org.voovan.test.http;

import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.server.HttpResponse;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class HttpClientTest {
    public static void main(String[] args) throws SendMessageException, ReadMessageException {
        HttpClient httpClient = new HttpClient("http://www.oschina.net","UTF-8",10000);
        Response resp = httpClient.send("/");
        Logger.simple(resp.body().getBodyString());
        httpClient.close();



        HttpClient httpClient1 = new HttpClient("http://www.oschina.net/","UTF-8",10000);
        Logger.simple(httpClient1.send("/").body().getBodyString());
        httpClient1.close();
    }
}

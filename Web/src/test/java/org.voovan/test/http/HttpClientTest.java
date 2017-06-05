package org.voovan.test.http;

import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
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
        HttpClient httpClient = null;
        try {
                httpClient = new HttpClient("http://127.0.0.1:2735/", "UTF-8", 50);
                httpClient.getParameters().put("all","1");
                Response resp = httpClient.send("/containers/json");
                Logger.simple(resp.body().getBodyString().substring(0,20));
            }catch(Exception e){
                e.printStackTrace();
            }finally {
                if(httpClient!=null) {
                    httpClient.close();
                }
            }
        Logger.simple("finished");
    }
}

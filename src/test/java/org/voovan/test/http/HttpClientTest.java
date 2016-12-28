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
                httpClient = new HttpClient("http://ddns.oray.com/", "UTF-8", 5);
                Response resp = httpClient.send("/checkip");
                Logger.simple(resp.protocol().getStatusCode());
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

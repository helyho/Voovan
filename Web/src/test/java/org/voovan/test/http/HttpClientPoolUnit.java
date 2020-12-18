package org.voovan.test.http;

import org.voovan.http.client.HttpClient;
import org.voovan.http.client.HttpClientPool;
import org.voovan.http.message.Response;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;

import java.util.Map;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpClientPoolUnit {
    @org.junit.Test
    public void te () throws SendMessageException, ReadMessageException {
        HttpClientPool httpClientPool = new HttpClientPool("http://127.0.0.1:8080", 30, 8, 8, 500);

        int baseUserId = 1000;
        int baseCurrencyId = 1000;

        for(int i=0;i<1000;i++) {
            HttpClient httpClient = httpClientPool.getHttpClient();
            System.out.println("===================" + i + "===================");
            httpClient.setMethod("GET");
            Response response = httpClient.send("/vjson");
            System.out.println("" + response.body().toString() + "      ");
            httpClientPool.restitution(httpClient);
        }

        TEnv.sleep(1000000);
    }
}
